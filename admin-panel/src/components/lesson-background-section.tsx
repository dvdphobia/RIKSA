import { useRef, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { ImagePlus, X } from "lucide-react";
import { assertMediaImage, uploadMedia } from "../lib/api";
import {
  LESSON_BACKGROUND_KEYS,
  type LessonBackgroundKey,
} from "../lib/types";
import { Button } from "./ui/button";

interface LessonBackgroundSectionProps {
  /** Lesson forms inherit a subject. Category forms are the subject. */
  inheritSubject: boolean;
  backgroundKey: string | null;
  backgroundImageUrl: string | null;
  subjectKey?: string | null;
  subjectImageUrl?: string | null;
  onBackgroundKey: (key: LessonBackgroundKey | null) => void;
  onBackgroundImageUrl: (url: string | null) => void;
}

const present = (value: string | null | undefined): string | null => {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
};

/** Same order as LessonBackgrounds.resolve: own value, else subject, else null. */
export function resolveLessonBackground(input: {
  backgroundKey: string | null;
  backgroundImageUrl: string | null;
  subjectKey?: string | null;
  subjectImageUrl?: string | null;
  inheritSubject: boolean;
}) {
  const ownKey = present(input.backgroundKey);
  const ownUrl = present(input.backgroundImageUrl);
  const backgroundKey = ownKey ?? (input.inheritSubject ? present(input.subjectKey) : null);
  const backgroundImageUrl = ownUrl ?? (input.inheritSubject ? present(input.subjectImageUrl) : null);
  return {
    backgroundKey,
    backgroundImageUrl,
    showsImage: backgroundImageUrl != null,
    keyFromSubject: ownKey == null && backgroundKey != null,
    imageFromSubject: ownUrl == null && backgroundImageUrl != null,
  };
}

const labelFor = (key: string | null) =>
  LESSON_BACKGROUND_KEYS.find((item) => item.key === key)?.label ?? key;

function Thumbnail({
  preset,
  large = false,
}: {
  preset: LessonBackgroundKey | "inherit";
  large?: boolean;
}) {
  const common = large ? "h-40 w-full" : "h-10 w-full";
  if (preset === "inherit") {
    return (
      <svg viewBox="0 0 80 48" className={common} aria-hidden>
        <rect x="4" y="4" width="72" height="40" rx="6" fill="none" stroke="currentColor" strokeDasharray="4 3" />
        <path d="M28 24h24M40 16v16" stroke="currentColor" strokeWidth="1.5" />
      </svg>
    );
  }
  if (preset === "none") {
    return (
      <svg viewBox="0 0 80 48" className={common} aria-hidden>
        <rect x="4" y="4" width="72" height="40" rx="6" className="fill-muted" />
        <path d="M22 14l36 20M58 14L22 34" stroke="currentColor" strokeWidth="1.5" />
      </svg>
    );
  }
  if (preset === "math") {
    return (
      <svg viewBox="0 0 80 48" className={common} aria-hidden>
        <rect width="80" height="48" rx="6" fill="#dbeafe" />
        <path d="M14 34 L30 14 L46 34 Z" fill="none" stroke="#1d4ed8" strokeWidth="2" />
        <path d="M50 16h16M50 24h16M50 32h10" stroke="#1d4ed8" strokeWidth="2" />
      </svg>
    );
  }
  if (preset === "physics") {
    return (
      <svg viewBox="0 0 80 48" className={common} aria-hidden>
        <rect width="80" height="48" rx="6" fill="#ede9fe" />
        <ellipse cx="40" cy="24" rx="18" ry="8" fill="none" stroke="#6d28d9" strokeWidth="1.5" />
        <ellipse cx="40" cy="24" rx="8" ry="18" fill="none" stroke="#6d28d9" strokeWidth="1.5" />
        <circle cx="40" cy="24" r="3" fill="#6d28d9" />
      </svg>
    );
  }
  if (preset === "chemistry") {
    return (
      <svg viewBox="0 0 80 48" className={common} aria-hidden>
        <rect width="80" height="48" rx="6" fill="#d1fae5" />
        <path d="M34 10h12v8l10 16a8 8 0 0 1-7 12H31a8 8 0 0 1-7-12l10-16z" fill="none" stroke="#047857" strokeWidth="1.5" />
        <path d="M32 30h16" stroke="#047857" strokeWidth="1.5" />
      </svg>
    );
  }
  if (preset === "biology") {
    return (
      <svg viewBox="0 0 80 48" className={common} aria-hidden>
        <rect width="80" height="48" rx="6" fill="#dcfce7" />
        <path d="M40 38c0-12 10-16 16-22-8 0-14 4-16 10-2-6-8-10-16-10 6 6 16 10 16 22z" fill="#15803d" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 80 48" className={common} aria-hidden>
      <rect width="80" height="48" rx="6" fill="#e2e8f0" />
      <rect x="18" y="12" width="44" height="24" rx="3" fill="#0f172a" />
      <path d="M24 20h10M24 26h18" stroke="#86efac" strokeWidth="1.5" />
    </svg>
  );
}

export function LessonBackgroundSection({
  inheritSubject,
  backgroundKey,
  backgroundImageUrl,
  subjectKey,
  subjectImageUrl,
  onBackgroundKey,
  onBackgroundImageUrl,
}: LessonBackgroundSectionProps) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const resolved = resolveLessonBackground({
    backgroundKey,
    backgroundImageUrl,
    subjectKey,
    subjectImageUrl,
    inheritSubject,
  });
  const selectedKey = present(backgroundKey);

  const upload = async (file: File | undefined) => {
    if (!file) return;
    try {
      assertMediaImage(file);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Image was rejected");
      return;
    }
    setUploading(true);
    try {
      const uploaded = await uploadMedia(file);
      onBackgroundImageUrl(uploaded.url);
      toast.success("Background image uploaded");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Image upload failed");
    } finally {
      setUploading(false);
    }
  };

  const caption = describeResolved(resolved, inheritSubject);

  return (
    <section className="space-y-4 rounded-md border bg-card p-6">
      <div>
        <h2 className="text-lg font-medium">Lesson background</h2>
        <p className="text-sm text-muted-foreground">
          {inheritSubject
            ? "A lesson value replaces the subject. Leave the preset on “Use subject default” to inherit it. An image is shown instead of a preset when both are set."
            : "This is the default for every lesson in the subject. “Use subject default” stores nothing, so the app uses its built-in art. An image is shown instead of a preset when both are set."}
        </p>
      </div>

      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        <PresetChoice
          label="Use subject default"
          selected={selectedKey == null}
          onClick={() => onBackgroundKey(null)}
        >
          <Thumbnail preset="inherit" />
        </PresetChoice>
        {LESSON_BACKGROUND_KEYS.map((item) => (
          <PresetChoice
            key={item.key}
            label={item.label}
            selected={selectedKey === item.key}
            onClick={() => onBackgroundKey(item.key)}
          >
            <Thumbnail preset={item.key} />
          </PresetChoice>
        ))}
      </div>

      <div className="space-y-2">
        <p className="text-sm font-medium">Image</p>
        <div className="flex flex-wrap items-center gap-3">
          {present(backgroundImageUrl) ? (
            <img
              src={present(backgroundImageUrl) ?? undefined}
              alt=""
              className="h-16 w-28 rounded-md border object-cover"
            />
          ) : (
            <div className="flex h-16 w-28 items-center justify-center rounded-md border border-dashed text-xs text-muted-foreground">
              No image
            </div>
          )}
          <input
            ref={fileRef}
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            className="hidden"
            onChange={(event) => {
              const input = event.currentTarget;
              const file = input.files?.[0];
              input.value = "";
              void upload(file);
            }}
          />
          <Button
            type="button"
            variant="outline"
            disabled={uploading}
            onClick={() => fileRef.current?.click()}
          >
            <ImagePlus className="mr-1 h-4 w-4" />
            {uploading ? "Uploading..." : "Upload image"}
          </Button>
          {present(backgroundImageUrl) && (
            <Button type="button" variant="ghost" onClick={() => onBackgroundImageUrl(null)}>
              <X className="mr-1 h-4 w-4" />
              Remove
            </Button>
          )}
        </div>
        <p className="text-xs text-muted-foreground">
          JPEG, PNG, GIF, or WebP. 2 MB maximum. Stored with the same media upload as lesson photos.
        </p>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-medium">Preview</p>
        <div className="flex aspect-video items-center justify-center overflow-hidden rounded-md border bg-muted">
          {resolved.showsImage ? (
            <img
              src={resolved.backgroundImageUrl ?? undefined}
              alt=""
              className="h-full w-full object-cover"
            />
          ) : resolved.backgroundKey && resolved.backgroundKey !== "none" ? (
            <div className="w-72">
              <Thumbnail preset={resolved.backgroundKey as LessonBackgroundKey} large />
            </div>
          ) : (
            <p className="px-6 text-center text-sm text-muted-foreground">
              {resolved.backgroundKey === "none"
                ? "No illustration"
                : "App subject illustration"}
            </p>
          )}
        </div>
        <p className="text-sm text-muted-foreground">{caption}</p>
      </div>
    </section>
  );
}

function describeResolved(
  resolved: ReturnType<typeof resolveLessonBackground>,
  inheritSubject: boolean,
) {
  if (resolved.showsImage) {
    const source = resolved.imageFromSubject ? "the subject image" : "this uploaded image";
    const keyNote = resolved.backgroundKey
      ? ` The ${labelFor(resolved.backgroundKey)} preset stays on the lesson for a failed image load and is not shown.`
      : "";
    return `Learners see ${source}.${keyNote}`;
  }
  if (resolved.backgroundKey === "none") {
    return resolved.keyFromSubject
      ? "Learners see no preset, because the subject is set to None."
      : "Learners see no preset. None turns the built-in illustration off.";
  }
  if (resolved.backgroundKey) {
    const source = resolved.keyFromSubject ? "the subject" : inheritSubject ? "this lesson" : "this subject";
    return `Learners see the ${labelFor(resolved.backgroundKey)} preset from ${source}.`;
  }
  return "Learners see the app's built-in subject illustration.";
}

function PresetChoice({
  label,
  selected,
  onClick,
  children,
}: {
  label: string;
  selected: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={`rounded-md border p-2 text-left text-xs ${
        selected ? "border-primary ring-2 ring-primary" : "hover:bg-accent"
      }`}
    >
      {children}
      <span className="mt-1 block font-medium">{label}</span>
    </button>
  );
}
