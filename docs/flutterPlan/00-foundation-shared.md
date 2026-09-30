# Foundation Sprint (Week 0) — Shared Scaffolding

> **Everyone depends on this. Build it first, together, before any feature work.**
> Lead: **Dev A**. Pair the others on the pieces they will rely on most.

The goal of Week 0 is to produce the **seams** described in `README.md` §5 so the four lanes can run in parallel without blocking each other.

---

## 0. Deliverables checklist

- [ ] Flutter project scaffolded with the folder structure from `README.md` §3
- [ ] `pubspec.yaml` with the agreed dependencies
- [ ] CI pipeline: `flutter analyze` + `flutter test` + build
- [ ] `AppTheme` (Material 3, light/dark, Khmer-capable fonts)
- [ ] `core/network`: dio client + auth interceptor + GraphQL client
- [ ] `core/storage`: secure token storage
- [ ] Auth seam: `authControllerProvider` + `AuthState`
- [ ] Router seam: root `go_router` that composes per-feature route lists
- [ ] `core/widgets`: shared Loading / Empty / Error / Button / Avatar
- [ ] Localization bootstrapped (`en`, `km`) with one sample string
- [ ] Each feature has a placeholder screen + `Fake<Repo>` wired in

---

## 1. Dependencies (`pubspec.yaml`)

```yaml
dependencies:
  flutter:
    sdk: flutter
  flutter_localizations:
    sdk: flutter

  # State
  flutter_riverpod: ^2.5.0
  riverpod_annotation: ^2.3.0

  # Navigation
  go_router: ^14.0.0

  # Networking
  dio: ^5.4.0
  graphql_flutter: ^5.1.2        # social module only

  # Models
  freezed_annotation: ^2.4.0
  json_annotation: ^4.9.0

  # Storage
  flutter_secure_storage: ^9.0.0
  shared_preferences: ^2.2.0

  # Utils
  intl: ^0.19.0

dev_dependencies:
  build_runner: ^2.4.0
  riverpod_generator: ^2.4.0
  freezed: ^2.5.0
  json_serializable: ^6.8.0
  flutter_lints: ^4.0.0
  mocktail: ^1.0.0
```

> Pin exact versions as a team once you run `flutter pub get` the first time, to keep everyone reproducible.

---

## 2. Environment config

```dart
// core/config/app_config.dart
class AppConfig {
  static const baseUrl = String.fromEnvironment(
    'BASE_URL',
    defaultValue: 'http://localhost:8080',
  );
  static String get apiBase => '$baseUrl/api/v1';
  static String get graphqlUrl => '$baseUrl/graphql';
}
```

Run with `flutter run --dart-define=BASE_URL=http://10.0.2.2:8080` (Android emulator → host).

---

## 3. Token storage (`core/storage/token_store.dart`)

```dart
class TokenStore {
  final _storage = const FlutterSecureStorage();
  static const _access = 'access_token';
  static const _refresh = 'refresh_token';

  Future<void> save({required String access, required String refresh}) async {
    await _storage.write(key: _access, value: access);
    await _storage.write(key: _refresh, value: refresh);
  }
  Future<String?> get accessToken => _storage.read(key: _access);
  Future<String?> get refreshToken => _storage.read(key: _refresh);
  Future<void> clear() => _storage.deleteAll();
}
```

---

## 4. REST client + auth interceptor (`core/network/dio_client.dart`)

This is the **most important shared piece**. It injects the bearer token and transparently refreshes on `401` using a single-flight lock so concurrent requests don't trigger multiple refreshes.

```dart
// Pseudocode — Dev A owns the real implementation.
Dio buildDio(TokenStore store, AuthController auth) {
  final dio = Dio(BaseOptions(baseUrl: AppConfig.apiBase));
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final token = await store.accessToken;
      if (token != null) options.headers['Authorization'] = 'Bearer $token';
      handler.next(options);
    },
    onError: (e, handler) async {
      if (e.response?.statusCode == 401) {
        final refreshed = await auth.refreshTokens(); // single-flight
        if (refreshed) {
          // retry original request with new token
          final clone = await dio.fetch(e.requestOptions);
          return handler.resolve(clone);
        }
        await auth.logout(); // refresh failed → kick to login
      }
      handler.next(e);
    },
  ));
  return dio;
}
```

**Token lifetimes (from backend):** access = 15 min, refresh = 30 days. Refresh endpoint returns a *new pair* of tokens — persist both.

---

## 5. GraphQL client (`core/network/graphql_client.dart`) — Dev B + Dev A

```dart
GraphQLClient buildGraphQLClient(TokenStore store) {
  final httpLink = HttpLink(AppConfig.graphqlUrl);
  final authLink = AuthLink(getToken: () async {
    final t = await store.accessToken;
    return t == null ? null : 'Bearer $t';
  });
  return GraphQLClient(
    link: authLink.concat(httpLink),
    cache: GraphQLCache(),
  );
}
```

> The GraphQL endpoint shares the same JWT. Note: it does **not** auto-refresh via the dio interceptor — Dev B must surface auth errors so the app can refresh/relogin. Coordinate with Dev A on a shared refresh helper.

---

## 6. Auth seam (`features/auth/.../auth_controller.dart`) — Dev A

The single source of truth for "am I logged in". Every other feature watches this.

```dart
sealed class AuthState {}
class AuthLoading   extends AuthState {}
class Authenticated extends AuthState { final AuthUser user; Authenticated(this.user); }
class Unauthenticated extends AuthState {}

// Exposed as: final authControllerProvider = ...
// Methods: login(), register(), googleLogin(), sendOtp(), verifyOtp(),
//          refreshTokens(), logout(), loadCurrentUser()
```

Other features **never** read tokens or call `/auth/*` — they only `ref.watch(authControllerProvider)` to know the user/session.

---

## 7. Router seam (`app/router/app_router.dart`) — Dev A composes, features contribute

Each feature exposes its routes; Dev A composes them. **No one else edits this file.**

```dart
// Each feature file, e.g. features/social/social_routes.dart:
List<RouteBase> socialRoutes() => [
  GoRoute(path: '/feed', builder: (c, s) => const FeedScreen()),
  GoRoute(path: '/post/:id', builder: (c, s) => PostDetailScreen(id: s.pathParameters['id']!)),
];

// app/router/app_router.dart (Dev A):
GoRouter buildRouter(Ref ref) => GoRouter(
  initialLocation: '/feed',
  redirect: (ctx, state) {
    final auth = ref.read(authControllerProvider);
    final loggedIn = auth is Authenticated;
    final onAuthPage = state.matchedLocation.startsWith('/auth');
    if (!loggedIn && !onAuthPage) return '/auth/login';
    if (loggedIn && onAuthPage) return '/feed';
    return null;
  },
  routes: [
    ...authRoutes(),
    ShellRoute(  // bottom-nav shell
      builder: (c, s, child) => AppShell(child: child),
      routes: [
        ...socialRoutes(),
        ...gamificationRoutes(),
        ...leaderboardRoutes(),
        ...blogRoutes(),
        ...learningRoutes(),
        ...profileRoutes(),
      ],
    ),
  ],
);
```

---

## 8. Repository pattern + fakes (every feature)

Each feature defines its repo interface and a fake **before** the real API call exists. This is what lets UI work proceed while the backend is incomplete (critical for social + learning).

```dart
// domain/post_repository.dart
abstract interface class PostRepository {
  Future<FeedPage> getFeed({String? categoryId, String? cursor, int limit});
  Future<Post> getPost(String id);
}

// data/fake_post_repository.dart — returns canned data, no network
class FakePostRepository implements PostRepository { ... }

// data/api_post_repository.dart — real GraphQL/dio calls
class ApiPostRepository implements PostRepository { ... }
```

A provider switches between fake and real via a build flag so the team can demo before integration:

```dart
final postRepositoryProvider = Provider<PostRepository>((ref) {
  return AppConfig.useFakes
      ? FakePostRepository()
      : ApiPostRepository(ref.watch(graphqlClientProvider));
});
```

---

## 9. Shared widgets (`core/widgets/`) — Dev A, with input from all

Build these once so every screen looks consistent:
- `AppScaffold` / `AppShell` (bottom navigation: Feed · Learn · Leaderboard · Blog · Profile)
- `LoadingView`, `EmptyView`, `ErrorView` (with retry)
- `PrimaryButton`, `AppTextField`
- `UserAvatar` (handles the default-avatar URL from backend)
- `AsyncValueWidget<T>` wrapper for Riverpod `AsyncValue` → loading/error/data

---

## 10. Localization (KM + EN)

- Set up `flutter gen-l10n` with `lib/l10n/app_en.arb` and `app_km.arb`.
- Khmer script needs a font that covers it (e.g. **Noto Sans Khmer**) — add to `pubspec.yaml` fonts and the theme.
- **Rule:** no hardcoded user-facing strings in any PR. Reviewers reject them.

---

## Exit criteria for Week 0

You're done when a developer can:
1. Clone, `flutter pub get`, `dart run build_runner build`, and run the app.
2. See a placeholder screen for **every** feature behind the bottom nav.
3. Log in with a real backend account and have the token persist across restarts.
4. Watch `authControllerProvider` from any feature.
5. Add a route to their feature without touching `app_router.dart`.

Once all five are true, split into the four lanes.
