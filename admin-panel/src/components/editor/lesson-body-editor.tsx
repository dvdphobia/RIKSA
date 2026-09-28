import { AlertTriangle, Eye, Hash, Pencil } from "lucide-react";
import { useMemo } from "react";
import { Alert, AlertDescription } from "../ui/alert";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Textarea } from "../ui/textarea";
import { LessonPreview } from "./lesson-preview";
import { lessonSections } from "./markdown";
import { RichTextEditor } from "./rich-text-editor";

const MAX_COMFORTABLE_SECTIONS = 5;

const SectionOutline = ({ markdown }: { markdown: string }) => {
  const sections = useMemo(() => lessonSections(markdown), [markdown]);
  const hasHeadings = /^##[ \t]+/m.test(markdown);

  return (
    <div className="space-y-2">
      {sections.length > 0 && (
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-xs font-medium text-muted-foreground">
            Learner sees:
          </span>
          {sections.map((section) => (
            <span
              key={section.index}
              className="inline-flex max-w-[16rem] items-center gap-1 rounded-full border bg-muted/50 px-2 py-0.5 text-xs"
            >
              <Hash className="h-3 w-3 shrink-0 text-muted-foreground" />
              <span className="truncate">
                {section.index === 4 ? "Final Review" : section.title}
              </span>
            </span>
          ))}
        </div>
      )}

      {sections.length > 0 && !hasHeadings && (
        <Alert>
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            This lesson is one long page. Use <strong>New section</strong> in the
            toolbar to split it — each section becomes its own screen with its
            own practice step.
          </AlertDescription>
        </Alert>
      )}

      {sections.length > MAX_COMFORTABLE_SECTIONS && (
        <Alert>
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            {sections.length} sections is a lot for one lesson — consider
            splitting it into two lessons.
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
};

interface LessonBodyEditorProps {
  value: string;
  onChange: (markdown: string) => void;
}

export const LessonBodyEditor = ({ value, onChange }: LessonBodyEditorProps) => (
  <Tabs defaultValue="write" className="space-y-3">
    <div className="flex items-center justify-between gap-4">
      <TabsList>
        <TabsTrigger value="write">
          <Pencil className="mr-1.5 h-3.5 w-3.5" /> Write
        </TabsTrigger>
        <TabsTrigger value="preview">
          <Eye className="mr-1.5 h-3.5 w-3.5" /> Preview
        </TabsTrigger>
        <TabsTrigger value="markdown">
          <Hash className="mr-1.5 h-3.5 w-3.5" /> Markdown
        </TabsTrigger>
      </TabsList>
    </div>

    <TabsContent value="write" className="space-y-3">
      <RichTextEditor value={value} onChange={onChange} />
      <SectionOutline markdown={value} />
    </TabsContent>

    <TabsContent value="preview">
      <LessonPreview markdown={value} />
    </TabsContent>

    <TabsContent value="markdown" className="space-y-2">
      <Textarea
        rows={20}
        className="font-mono text-xs"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <p className="text-xs text-muted-foreground">
        The raw source stored by the API. Edit here only if you know markdown —
        the Write tab is easier.
      </p>
    </TabsContent>
  </Tabs>
);
