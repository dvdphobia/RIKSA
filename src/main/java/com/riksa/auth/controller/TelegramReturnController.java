package com.riksa.auth.controller;

import com.riksa.common.constants.AuthEndpoints;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The page behind the bot's "Back to Riksa" button.
 *
 * Telegram buttons only open https links, so this page hands over to the app's
 * own {@code riksa://} link. The app is already waiting on the sign-in screen
 * and finishes as soon as it comes back to the front.
 */
@RestController
@RequestMapping(AuthEndpoints.BASE)
public class TelegramReturnController {

    static final String APP_LINK = "riksa://auth/telegram";
    static final String ANDROID_LINK =
            "intent://auth/telegram#Intent;scheme=riksa;package=com.riksa.riksa_app;end";

    private static final String PAGE = """
            <!doctype html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Back to Riksa</title>
            <style>
              body { margin: 0; min-height: 100vh; display: grid; place-items: center;
                     font-family: system-ui, -apple-system, sans-serif; background: #f6f5fb; color: #1d1b26; }
              main { text-align: center; padding: 24px; max-width: 360px; }
              h1 { font-size: 20px; margin: 0 0 8px; }
              p { color: #5e5a6e; margin: 0 0 24px; line-height: 1.5; }
              a { display: inline-block; padding: 14px 28px; border-radius: 999px; background: #6c4ce0;
                  color: #fff; font-weight: 600; text-decoration: none; }
            </style>
            </head>
            <body>
            <main>
              <h1>✅ Confirmed</h1>
              <p>Opening Riksa… If nothing happens, tap the button.</p>
              <a id="open" href="%s">Open Riksa</a>
            </main>
            <script>
              var link = /Android/i.test(navigator.userAgent) ? "%s" : "%s";
              document.getElementById("open").href = link;
              window.location.href = link;
            </script>
            </body>
            </html>
            """.formatted(APP_LINK, ANDROID_LINK, APP_LINK);

    @GetMapping(value = AuthEndpoints.TELEGRAM_RETURN, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> backToApp() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PAGE);
    }
}
