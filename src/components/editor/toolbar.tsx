import { useEditorState, type Editor } from "@tiptap/react";
import {
  Bold,
  Code,
  Code2,
  Heading2,
  Heading3,
  Image as ImageIcon,
  Italic,
  Link as LinkIcon,
  List,
  ListOrdered,
  Minus,
  Pilcrow,
  Quote,
  Redo2,
  RemoveFormatting,
  Strikethrough,
  Table as TableIcon,
  Undo2,
  Unlink,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { cn } from "../../lib/utils";
import { Button } from "../ui/button";
import { Separator } from "../ui/separator";

interface ToolButtonProps {
  icon: LucideIcon;
  label: string;
  active?: boolean;
  disabled?: boolean;
  onClick: () => void;
  wide?: boolean;
}

const ToolButton = ({
  icon: Icon,
  label,
  active,
  disabled,
  onClick,
  wide,
}: ToolButtonProps) => (
  <Button
    type="button"
    variant="ghost"
    size="sm"
    title={label}
    aria-label={label}
    aria-pressed={active}
    disabled={disabled}
    // Keep the caret where it is: without this the button steals focus from the
    // editor on mousedown and the command lands on a stale selection.
    onMouseDown={(event) => event.preventDefault()}
    onClick={onClick}
    className={cn(
      "h-8 px-2 text-muted-foreground",
      active && "bg-accent text-accent-foreground",
      !wide && "w-8",
    )}
  >
    <Icon className="h-4 w-4" />
    {wide && <span className="ml-1.5 text-xs font-medium">{label}</span>}
  </Button>
);

export const EditorToolbar = ({ editor }: { editor: Editor }) => {
  // `useEditor` does not re-render on every transaction in Tiptap v3, so the
  // button states have to be subscribed to explicitly or they freeze.
  const state = useEditorState({
    editor,
    selector: ({ editor: e }) => ({
      heading2: e.isActive("heading", { level: 2 }),
      heading3: e.isActive("heading", { level: 3 }),
      paragraph: e.isActive("paragraph"),
      bold: e.isActive("bold"),
      italic: e.isActive("italic"),
      strike: e.isActive("strike"),
      code: e.isActive("code"),
      bulletList: e.isActive("bulletList"),
      orderedList: e.isActive("orderedList"),
      blockquote: e.isActive("blockquote"),
      codeBlock: e.isActive("codeBlock"),
      link: e.isActive("link"),
      canUndo: e.can().undo(),
      canRedo: e.can().redo(),
    }),
  });

  const setLink = () => {
    const current = editor.getAttributes("link").href as string | undefined;
    const href = window.prompt("Link address (https://...)", current ?? "https://");
    if (href === null) return;
    if (!href.trim()) {
      editor.chain().focus().unsetLink().run();
      return;
    }
    editor
      .chain()
      .focus()
      .extendMarkRange("link")
      .setLink({ href: href.trim() })
      .run();
  };

  const addImage = () => {
    const src = window.prompt("Image address (https://...)");
    if (!src?.trim()) return;
    const alt = window.prompt("Describe the image (shown if it fails to load)") ?? "";
    editor.chain().focus().setImage({ src: src.trim(), alt }).run();
  };

  return (
    <div className="flex flex-wrap items-center gap-0.5 border-b bg-muted/40 p-1.5">
      <ToolButton
        icon={Heading2}
        label="New section"
        wide
        active={state.heading2}
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
      />
      <ToolButton
        icon={Heading3}
        label="Sub-heading"
        active={state.heading3}
        onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
      />
      <ToolButton
        icon={Pilcrow}
        label="Normal text"
        active={state.paragraph}
        onClick={() => editor.chain().focus().setParagraph().run()}
      />

      <Separator orientation="vertical" className="mx-1 h-6" />

      <ToolButton
        icon={Bold}
        label="Bold"
        active={state.bold}
        onClick={() => editor.chain().focus().toggleBold().run()}
      />
      <ToolButton
        icon={Italic}
        label="Italic"
        active={state.italic}
        onClick={() => editor.chain().focus().toggleItalic().run()}
      />
      <ToolButton
        icon={Strikethrough}
        label="Strikethrough"
        active={state.strike}
        onClick={() => editor.chain().focus().toggleStrike().run()}
      />
      <ToolButton
        icon={Code}
        label="Code word"
        active={state.code}
        onClick={() => editor.chain().focus().toggleCode().run()}
      />
      <ToolButton
        icon={RemoveFormatting}
        label="Clear formatting"
        onClick={() =>
          editor.chain().focus().unsetAllMarks().clearNodes().run()
        }
      />

      <Separator orientation="vertical" className="mx-1 h-6" />

      <ToolButton
        icon={List}
        label="Bullet list"
        active={state.bulletList}
        onClick={() => editor.chain().focus().toggleBulletList().run()}
      />
      <ToolButton
        icon={ListOrdered}
        label="Numbered list"
        active={state.orderedList}
        onClick={() => editor.chain().focus().toggleOrderedList().run()}
      />
      <ToolButton
        icon={Quote}
        label="Quote"
        active={state.blockquote}
        onClick={() => editor.chain().focus().toggleBlockquote().run()}
      />
      <ToolButton
        icon={Code2}
        label="Code block"
        active={state.codeBlock}
        onClick={() => editor.chain().focus().toggleCodeBlock().run()}
      />
      <ToolButton
        icon={Minus}
        label="Divider"
        onClick={() => editor.chain().focus().setHorizontalRule().run()}
      />

      <Separator orientation="vertical" className="mx-1 h-6" />

      <ToolButton
        icon={LinkIcon}
        label="Add link"
        active={state.link}
        onClick={setLink}
      />
      <ToolButton
        icon={Unlink}
        label="Remove link"
        disabled={!state.link}
        onClick={() => editor.chain().focus().unsetLink().run()}
      />
      <ToolButton icon={ImageIcon} label="Insert image" onClick={addImage} />
      <ToolButton
        icon={TableIcon}
        label="Insert table"
        onClick={() =>
          editor
            .chain()
            .focus()
            .insertTable({ rows: 3, cols: 3, withHeaderRow: true })
            .run()
        }
      />

      <Separator orientation="vertical" className="mx-1 h-6" />

      <ToolButton
        icon={Undo2}
        label="Undo"
        disabled={!state.canUndo}
        onClick={() => editor.chain().focus().undo().run()}
      />
      <ToolButton
        icon={Redo2}
        label="Redo"
        disabled={!state.canRedo}
        onClick={() => editor.chain().focus().redo().run()}
      />
    </div>
  );
};
