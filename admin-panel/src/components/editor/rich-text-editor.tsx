import { Image } from "@tiptap/extension-image";
import { TableKit } from "@tiptap/extension-table";
import { Placeholder } from "@tiptap/extensions";
import { EditorContent, useEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { useEffect, useRef } from "react";
import { cn } from "../../lib/utils";
import { htmlToMarkdown, isEmptyHtml, markdownToHtml } from "./markdown";
import { EditorToolbar } from "./toolbar";

interface RichTextEditorProps {
  /** Markdown source. */
  value: string;
  /** Fired only for edits the author actually makes, never for programmatic loads. */
  onChange: (markdown: string) => void;
  placeholder?: string;
  className?: string;
}

export const RichTextEditor = ({
  value,
  onChange,
  placeholder = "Write the lesson here. Use “New section” to start a new page for the learner.",
  className,
}: RichTextEditorProps) => {
  // The markdown the editor's current document corresponds to. Used to tell an
  // incoming prop change apart from an echo of what we just emitted.
  const syncedMarkdown = useRef(value);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        // Markdown has no underline, so don't let authors create one.
        underline: false,
        link: { openOnClick: false, autolink: true },
      }),
      Image.configure({ inline: false }),
      TableKit.configure({ table: { resizable: true } }),
      Placeholder.configure({ placeholder }),
    ],
    content: markdownToHtml(value),
    editorProps: {
      attributes: {
        class: "lesson-content min-h-[320px] px-4 py-3 focus:outline-none",
      },
    },
    onUpdate: ({ editor: instance }) => {
      const html = instance.getHTML();
      const markdown = isEmptyHtml(html) ? "" : htmlToMarkdown(html);
      syncedMarkdown.current = markdown;
      onChange(markdown);
    },
  });

  useEffect(() => {
    if (!editor || value === syncedMarkdown.current) return;
    syncedMarkdown.current = value;
    editor.commands.setContent(markdownToHtml(value), { emitUpdate: false });
  }, [editor, value]);

  if (!editor) return null;

  return (
    <div
      className={cn(
        "overflow-hidden rounded-md border bg-background focus-within:ring-1 focus-within:ring-ring",
        className,
      )}
    >
      <EditorToolbar editor={editor} />
      <EditorContent editor={editor} />
    </div>
  );
};
