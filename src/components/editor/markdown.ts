import DOMPurify from "dompurify";
import { marked } from "marked";
import TurndownService from "turndown";

/**
 * Conversion layer between the visual editor (HTML) and what the API stores
 * (markdown). The mobile app renders lesson bodies with `flutter_markdown_plus`
 * using the default GitHub-flavoured extension set and `softLineBreak: true`,
 * so a single newline is a line break — `breaks` below mirrors that on both
 * sides of the round trip.
 */

marked.setOptions({ gfm: true, breaks: true });

export const markdownToHtml = (markdown: string): string =>
  marked.parse(markdown ?? "", { async: false });

/**
 * For preview only. The app's markdown renderer ignores raw HTML entirely, so
 * stripping anything markdown itself did not produce is both safer (lesson
 * bodies can arrive from `.md` uploads) and closer to what the learner sees.
 */
export const markdownToPreviewHtml = (markdown: string): string =>
  DOMPurify.sanitize(markdownToHtml(markdown), {
    ALLOWED_TAGS: [
      "p", "br", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
      "strong", "em", "del", "s", "code", "pre", "blockquote",
      "ul", "ol", "li", "a", "img",
      "table", "thead", "tbody", "tr", "th", "td",
    ],
    ALLOWED_ATTR: ["href", "src", "alt", "title"],
    ALLOWED_URI_REGEXP: /^(?:https?:|mailto:|tel:|#|\/)/i,
  });

const turndown = new TurndownService({
  headingStyle: "atx",
  bulletListMarker: "-",
  codeBlockStyle: "fenced",
  emDelimiter: "*",
  strongDelimiter: "**",
  hr: "---",
  linkStyle: "inlined",
});

// A soft break in the editor is a soft break in the app, not a hard `  \n`.
turndown.addRule("lineBreak", {
  filter: "br",
  replacement: () => "\n",
});

// turndown's default pads markers to four columns (`-   item`), which reads as
// noise in the Markdown tab. One space, keeping the four-space continuation
// indent so nested lists still parse.
turndown.addRule("listItem", {
  filter: "li",
  replacement: (content, node) => {
    const body = content
      .replace(/^\n+/, "")
      .replace(/\n+$/, "\n")
      .replace(/\n/gm, "\n    ");
    const parent = node.parentNode as HTMLElement | null;
    let prefix = "- ";
    if (parent?.nodeName === "OL") {
      const start = parent.getAttribute("start");
      const index = Array.prototype.indexOf.call(parent.children, node);
      prefix = `${start ? Number(start) + index : index + 1}. `;
    }
    const trailingNewline =
      node.nextSibling && !/\n$/.test(body) ? "\n" : "";
    return prefix + body + trailingNewline;
  },
});

turndown.addRule("strikethrough", {
  filter: ["del", "s"],
  replacement: (content) => `~~${content}~~`,
});

const cellToMarkdown = (cell: Element): string =>
  turndown
    .turndown(cell.innerHTML)
    .replace(/\n+/g, " ")
    .replace(/\|/g, "\\|")
    .trim();

// turndown drops tables by default; the editor only produces well-formed ones.
turndown.addRule("table", {
  filter: "table",
  replacement: (_content, node) => {
    const rows = Array.from((node as HTMLTableElement).rows);
    if (!rows.length) return "";
    const cells = rows.map((row) => Array.from(row.cells).map(cellToMarkdown));
    const width = Math.max(...cells.map((row) => row.length));
    const pad = (row: string[]) =>
      `| ${Array.from({ length: width }, (_, i) => row[i] ?? "").join(" | ")} |`;
    const [header, ...body] = cells;
    const divider = `| ${Array.from({ length: width }, () => "---").join(" | ")} |`;
    return `\n\n${[pad(header), divider, ...body.map(pad)].join("\n")}\n\n`;
  },
});

turndown.remove(["style", "script"]);

export const htmlToMarkdown = (html: string): string =>
  turndown
    .turndown(html ?? "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();

/** An empty TipTap document still serialises to a paragraph. */
export const isEmptyHtml = (html: string): boolean =>
  !html || html.replace(/<[^>]*>/g, "").replace(/&nbsp;|\s/g, "") === "";

export interface LessonSection {
  index: number;
  title: string;
  content: string;
}

const SECTION_HEADING = /^##[ \t]+(.+)$/gm;

/**
 * Mirrors `_markdownSections` / `_sectionTitle` in the app's
 * `lesson_experience_screen.dart`: every `## ` heading starts a new section,
 * and each section becomes its own screen for the learner.
 */
export const lessonSections = (markdown: string): LessonSection[] => {
  const source = markdown ?? "";
  const matches = Array.from(source.matchAll(SECTION_HEADING));
  if (!matches.length) {
    // The app falls back to the title "Practice" for a section with no heading.
    return source.trim()
      ? [{ index: 1, title: "Practice", content: source.trim() }]
      : [];
  }
  return matches.map((match, i) => {
    const start = match.index ?? 0;
    const end = i + 1 < matches.length ? (matches[i + 1].index ?? source.length) : source.length;
    return {
      index: i + 1,
      title: match[1].trim(),
      content: source.slice(start, end).trim(),
    };
  });
};
