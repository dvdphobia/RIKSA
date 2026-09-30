import { BookOpen } from "lucide-react";
import { lessonSections, markdownToPreviewHtml } from "./markdown";

/** The app labels section 4 "Final Review" regardless of its heading. */
const sectionLabel = (index: number, title: string) =>
  index === 4 ? "Final Review" : `Section ${index}: ${title}`;

export const LessonPreview = ({ markdown }: { markdown: string }) => {
  const sections = lessonSections(markdown);

  if (!sections.length) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-md border border-dashed px-4 py-12 text-center text-sm text-muted-foreground">
        <BookOpen className="h-5 w-5" />
        Nothing to preview yet — write the lesson first.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {sections.map((section) => (
        <div key={section.index} className="rounded-md border bg-background">
          <div className="border-b bg-muted/40 px-4 py-2">
            <p className="text-sm font-semibold">
              {sectionLabel(section.index, section.title)}
            </p>
            <p className="text-xs text-muted-foreground">
              {section.index === 4
                ? "Lesson challenge: apply every idea you have learned."
                : "Read the idea, then prove it in practice."}
            </p>
          </div>
          <div
            className="lesson-content px-4 py-3"
            // Sanitised in markdownToPreviewHtml.
            dangerouslySetInnerHTML={{
              __html: markdownToPreviewHtml(section.content),
            }}
          />
        </div>
      ))}
    </div>
  );
};
