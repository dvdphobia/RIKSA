# Learning API

Student app contract for lesson pages. Admin writes are in [ADMIN_API.md](ADMIN_API.md). Paths below are under `/api/v1`.

`GET /lessons/**` does not require a token. A `Bearer` token still personalizes progress fields.

## Background fields

Every lesson payload the app reads includes two resolved strings:

| Field | Type | Meaning |
|---|---|---|
| `backgroundKey` | string or null | Preset illustration shipped in the app |
| `backgroundImageUrl` | string or null | Uploaded image URL |

Allowed keys: `math`, `physics`, `chemistry`, `biology`, `computer_science`, `none`.

Resolution, per field:

1. The lesson's own value, when it is non-null and non-blank.
2. Otherwise the lesson's category (subject) value.
3. Otherwise `null`.

`none` is a real lesson value. It does not fall through to the subject key.

Paint rule when both resolved fields are set: **the image wins**. Show `backgroundImageUrl`. Keep `backgroundKey` in the payload so a failed image load can still use the preset. Use the key only when `backgroundImageUrl` is null.

- `backgroundKey` is a preset (not `none`) and `backgroundImageUrl` is null: draw that preset.
- `backgroundKey` is `none` and `backgroundImageUrl` is null: draw no preset.
- Both are null: the app uses its own subject illustration.

Admin create/update stores the raw lesson or category columns. Those responses are not resolved. See ADMIN_API.md sections 7 and 8.

## `GET /lessons` — catalog

`?categoryId=&difficulty=&page=0&size=20`

Each item in `content` is a lesson summary. Other fields are unchanged. The background fields are resolved:

```json
{
  "id": "0b1d6a4e-6b1a-4a3e-9c1a-2f0e5a7b9c11",
  "title": "Forces",
  "description": "Newton's laws",
  "difficulty": "EASY",
  "baseXp": 10,
  "position": 2,
  "categoryId": "6a1c2d3e-4f50-4a61-8b72-93c4d5e6f708",
  "categoryName": "Physics",
  "backgroundKey": "physics",
  "backgroundImageUrl": null,
  "exerciseCount": 4,
  "completed": false,
  "state": "AVAILABLE",
  "createdAt": "2026-11-13T09:00:00Z"
}
```

This lesson stored no override. `physics` came from the subject.

## `GET /lessons/{id}` — lesson page

```json
{
  "id": "0b1d6a4e-6b1a-4a3e-9c1a-2f0e5a7b9c11",
  "title": "Forces",
  "description": "Newton's laws",
  "difficulty": "EASY",
  "baseXp": 10,
  "position": 2,
  "categoryId": "6a1c2d3e-4f50-4a61-8b72-93c4d5e6f708",
  "categoryName": "Physics",
  "reflectionPrompt": "What surprised you?",
  "backgroundKey": "physics",
  "backgroundImageUrl": "https://api.example/api/v1/media/4f3c1a2b-1111-4222-8333-555566667777.png",
  "state": "AVAILABLE",
  "exercises": [],
  "createdAt": "2026-11-13T09:00:00Z",
  "updatedAt": "2026-11-13T09:30:00Z"
}
```

Here the lesson (or its subject) resolved both a key and an image. The page paints the image.

A lesson override of `"backgroundKey": "none"` with no image on the lesson or the subject:

```json
{ "backgroundKey": "none", "backgroundImageUrl": null }
```

## `GET /lessons/roadmap?categoryId=` — roadmap

Each object in `units[].lessons` is a lesson summary and includes the same resolved fields:

```json
{
  "id": "0b1d6a4e-6b1a-4a3e-9c1a-2f0e5a7b9c11",
  "title": "Forces",
  "description": "Newton's laws",
  "difficulty": "EASY",
  "baseXp": 10,
  "rewardXp": 40,
  "estimatedMinutes": 8,
  "exerciseCount": 4,
  "progress": 0.0,
  "status": "AVAILABLE",
  "current": true,
  "backgroundKey": "math",
  "backgroundImageUrl": null
}
```

`math` here is a lesson override. The subject default is not used for the key. `backgroundImageUrl` is null because neither the lesson nor the subject has an image, so the app draws the Math preset.
