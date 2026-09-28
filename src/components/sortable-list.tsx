import { ArrowDown, ArrowUp, GripVertical } from "lucide-react";
import { type ReactNode, useState } from "react";
import { cn } from "../lib/utils";
import { Button } from "./ui/button";

/**
 * Vertical list reordered by dragging the grip or with the arrow buttons.
 * Calls onChange with the new order; the parent decides when to save.
 */
export function SortableList<T>({
  items,
  getId,
  onChange,
  renderItem,
  disabled = false,
}: {
  items: T[];
  getId: (item: T) => string;
  onChange: (next: T[]) => void;
  renderItem: (item: T, index: number) => ReactNode;
  disabled?: boolean;
}) {
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [overIndex, setOverIndex] = useState<number | null>(null);

  const move = (from: number, to: number) => {
    if (from === to || to < 0 || to >= items.length) return;
    const next = [...items];
    const [moved] = next.splice(from, 1);
    next.splice(to, 0, moved);
    onChange(next);
  };

  return (
    <ol className="divide-y rounded-md border bg-card">
      {items.map((item, index) => (
        <li
          key={getId(item)}
          draggable={!disabled}
          onDragStart={(e) => {
            setDragIndex(index);
            e.dataTransfer.effectAllowed = "move";
          }}
          onDragOver={(e) => {
            e.preventDefault();
            setOverIndex(index);
          }}
          onDragEnd={() => {
            setDragIndex(null);
            setOverIndex(null);
          }}
          onDrop={(e) => {
            e.preventDefault();
            if (dragIndex !== null) move(dragIndex, index);
            setDragIndex(null);
            setOverIndex(null);
          }}
          className={cn(
            "flex items-center gap-2 px-2 py-2",
            dragIndex === index && "opacity-40",
            overIndex === index && dragIndex !== index && "bg-accent",
          )}
        >
          <GripVertical
            className={cn(
              "h-4 w-4 shrink-0 text-muted-foreground",
              !disabled && "cursor-grab",
            )}
          />
          <span className="w-6 shrink-0 text-right font-mono text-xs text-muted-foreground">
            {index + 1}
          </span>
          <div className="min-w-0 flex-1">{renderItem(item, index)}</div>
          <Button
            type="button"
            size="icon"
            variant="ghost"
            className="h-7 w-7"
            disabled={disabled || index === 0}
            onClick={() => move(index, index - 1)}
            aria-label="Move up"
          >
            <ArrowUp className="h-3 w-3" />
          </Button>
          <Button
            type="button"
            size="icon"
            variant="ghost"
            className="h-7 w-7"
            disabled={disabled || index === items.length - 1}
            onClick={() => move(index, index + 1)}
            aria-label="Move down"
          >
            <ArrowDown className="h-3 w-3" />
          </Button>
        </li>
      ))}
    </ol>
  );
}
