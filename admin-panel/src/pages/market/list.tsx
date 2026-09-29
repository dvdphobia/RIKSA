import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getCoreRowModel,
  getPaginationRowModel,
  type ColumnDef,
  useReactTable,
} from "@tanstack/react-table";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { formatDate, PageHeader, StatusBadge, toLocalInput, fromLocalInput, errorMessage } from "../../components/shared";
import { DataTable } from "../../components/table/data-table";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { apiFetch } from "../../lib/api";
import type {
  AdminMarketItem,
  AdminMarketItemRequest,
  AdminMarketRarity,
  AdminMarketSlot,
  RankTierConfig,
} from "../../lib/types";

interface MarketDraft {
  id: string | null;
  slot: AdminMarketSlot;
  name: string;
  nameKm: string;
  price: string;
  rarity: AdminMarketRarity;
  assetKey: string;
  speciesMask: string;
  minTier: string;
  availableFrom: string;
  availableUntil: string;
  sortOrder: string;
  active: boolean;
}

const emptyDraft = (): MarketDraft => ({
  id: null,
  slot: "HAT",
  name: "",
  nameKm: "",
  price: "0",
  rarity: "COMMON",
  assetKey: "",
  speciesMask: "",
  minTier: "",
  availableFrom: "",
  availableUntil: "",
  sortOrder: "0",
  active: true,
});

const toDraft = (item: AdminMarketItem): MarketDraft => ({
  id: item.id,
  slot: item.slot,
  name: item.name,
  nameKm: item.nameKm,
  price: String(item.price),
  rarity: item.rarity,
  assetKey: item.assetKey,
  speciesMask: item.speciesMask ?? "",
  minTier: item.minTier ?? "",
  availableFrom: toLocalInput(item.availableFrom),
  availableUntil: toLocalInput(item.availableUntil),
  sortOrder: String(item.sortOrder),
  active: item.active,
});

const validateDraft = (draft: MarketDraft, tiers: RankTierConfig[]): string | null => {
  if (!draft.name.trim()) return "Name is required.";
  if (draft.name.trim().length > 80) return "Name must be 80 characters or fewer.";
  if (!draft.nameKm.trim()) return "Khmer name is required.";
  if (draft.nameKm.trim().length > 120) return "Khmer name must be 120 characters or fewer.";
  const price = Number(draft.price);
  if (!draft.price.trim() || !Number.isSafeInteger(price) || price < 0) return "Price must be a whole number of 0 or more.";
  if (!/^[a-z0-9_]{1,64}$/.test(draft.assetKey)) {
    return "Asset key must use 1–64 lowercase letters, digits, or underscores.";
  }
  const speciesMask = draft.speciesMask.trim();
  if (speciesMask && !/^(crocodile|capybara)(\s*,\s*(crocodile|capybara))*$/i.test(speciesMask)) {
    return "Species mask must be a comma-separated list of crocodile and/or capybara.";
  }
  if (draft.minTier.length > 32) return "Minimum tier must be 32 characters or fewer.";
  if (draft.minTier && !tiers.some((tier) => tier.tierName.toLowerCase() === draft.minTier.toLowerCase())) {
    return "Minimum tier must match an existing rank tier.";
  }
  const sortOrder = Number(draft.sortOrder);
  if (!draft.sortOrder.trim() || !Number.isInteger(sortOrder) || sortOrder < -2147483648 || sortOrder > 2147483647) {
    return "Sort order must be a whole number.";
  }
  const start = draft.availableFrom ? new Date(draft.availableFrom).getTime() : null;
  const end = draft.availableUntil ? new Date(draft.availableUntil).getTime() : null;
  if ((start !== null && !Number.isFinite(start)) || (end !== null && !Number.isFinite(end))) {
    return "Availability dates are invalid.";
  }
  if (start !== null && end !== null && end <= start) {
    return "Availability end must be after availability start.";
  }
  return null;
};

const toRequest = (draft: MarketDraft): AdminMarketItemRequest => ({
  slot: draft.slot,
  name: draft.name.trim(),
  nameKm: draft.nameKm.trim(),
  price: Number(draft.price),
  rarity: draft.rarity,
  assetKey: draft.assetKey,
  speciesMask: draft.speciesMask.trim() || null,
  minTier: draft.minTier || null,
  availableFrom: fromLocalInput(draft.availableFrom),
  availableUntil: fromLocalInput(draft.availableUntil),
  sortOrder: Number(draft.sortOrder),
  active: draft.active,
});

export const MarketItemList = () => {
  const queryClient = useQueryClient();
  const [slot, setSlot] = useState<AdminMarketSlot | undefined>();
  const [active, setActive] = useState<boolean | undefined>();
  const [editing, setEditing] = useState<MarketDraft | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminMarketItem | null>(null);

  const items = useQuery({
    queryKey: ["market", "items", slot, active],
    queryFn: () => apiFetch<AdminMarketItem[]>("/admin/market/items", { params: { slot, active } }),
  });
  const tiers = useQuery({
    queryKey: ["gamification", "rank-tiers"],
    queryFn: () => apiFetch<RankTierConfig[]>("/admin/gamification/rank-tiers"),
  });

  const save = useMutation({
    mutationFn: (draft: MarketDraft) =>
      apiFetch<AdminMarketItem>(
        draft.id ? `/admin/market/items/${draft.id}` : "/admin/market/items",
        { method: draft.id ? "PUT" : "POST", body: toRequest(draft) },
      ),
    onSuccess: () => {
      toast.success(editing?.id ? "Market item saved" : "Market item created");
      setEditing(null);
      setFormError(null);
      queryClient.invalidateQueries({ queryKey: ["market", "items"] });
    },
    onError: (error) => toast.error(errorMessage(error)),
  });
  const remove = useMutation({
    mutationFn: (id: string) =>
      apiFetch<AdminMarketItem | undefined>(`/admin/market/items/${id}`, { method: "DELETE" }),
    onSuccess: (item) => {
      toast.success(item ? "Item deactivated; existing owners keep it." : "Item deleted");
      setDeleteTarget(null);
      queryClient.invalidateQueries({ queryKey: ["market", "items"] });
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  const columns = useMemo<ColumnDef<AdminMarketItem>[]>(
    () => [
      { id: "slot", accessorKey: "slot", header: "Slot", cell: ({ row }) => <StatusBadge value={row.original.slot} /> },
      {
        id: "name",
        header: "Name",
        cell: ({ row }) => (
          <div>
            <p className="font-medium">{row.original.name}</p>
            <p className="text-sm text-muted-foreground">{row.original.nameKm}</p>
          </div>
        ),
      },
      { id: "price", accessorKey: "price", header: "Price", cell: ({ row }) => row.original.price.toLocaleString() },
      { id: "rarity", accessorKey: "rarity", header: "Rarity", cell: ({ row }) => <StatusBadge value={row.original.rarity} /> },
      { id: "assetKey", accessorKey: "assetKey", header: "Asset key", cell: ({ row }) => <code className="text-xs">{row.original.assetKey}</code> },
      { id: "active", header: "Active", cell: ({ row }) => <StatusBadge value={row.original.active ? "ACTIVE" : "INACTIVE"} /> },
      {
        id: "availability",
        header: "Availability window",
        cell: ({ row }) => (
          <span className="whitespace-nowrap text-xs text-muted-foreground">
            {row.original.availableFrom ? formatDate(row.original.availableFrom) : "Any start"}
            <br />
            {row.original.availableUntil ? formatDate(row.original.availableUntil) : "No end"}
          </span>
        ),
      },
      {
        id: "counts",
        header: "Owned / equipped",
        cell: ({ row }) => `${row.original.ownedCount.toLocaleString()} / ${row.original.equippedCount.toLocaleString()}`,
      },
      {
        id: "actions",
        header: "",
        cell: ({ row }) => (
          <div className="flex justify-end gap-1">
            <Button size="icon" variant="ghost" title="Edit" onClick={() => { setEditing(toDraft(row.original)); setFormError(null); }}>
              <Pencil className="h-4 w-4" />
            </Button>
            <Button size="icon" variant="ghost" title="Delete or deactivate" onClick={() => setDeleteTarget(row.original)}>
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          </div>
        ),
      },
    ],
    [],
  );
  const table = useReactTable({
    data: items.data ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
  });

  const submit = () => {
    if (!editing) return;
    const error = validateDraft(editing, tiers.data ?? []);
    setFormError(error);
    if (!error) save.mutate(editing);
  };

  return (
    <div>
      <PageHeader
        title="Market items"
        description="Manage buddy wardrobe items and their availability"
        actions={
          <Button onClick={() => { setEditing(emptyDraft()); setFormError(null); }}>
            <Plus className="mr-1 h-4 w-4" /> New item
          </Button>
        }
      />
      <div className="mb-4 flex flex-wrap gap-2">
        <Select value={slot ?? "all"} onValueChange={(value) => setSlot(value === "all" ? undefined : value as AdminMarketSlot)}>
          <SelectTrigger className="w-40"><SelectValue placeholder="Slot" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All slots</SelectItem>
            <SelectItem value="HAT">HAT</SelectItem>
            <SelectItem value="EYEWEAR">EYEWEAR</SelectItem>
          </SelectContent>
        </Select>
        <Select value={active === undefined ? "all" : String(active)} onValueChange={(value) => setActive(value === "all" ? undefined : value === "true")}>
          <SelectTrigger className="w-40"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All items</SelectItem>
            <SelectItem value="true">Active</SelectItem>
            <SelectItem value="false">Inactive</SelectItem>
          </SelectContent>
        </Select>
      </div>
      {items.isLoading && <p className="py-8 text-center text-sm text-muted-foreground">Loading market items…</p>}
      {items.isError && <p className="py-8 text-center text-sm text-destructive">{errorMessage(items.error, "Could not load market items")}</p>}
      {!items.isLoading && !items.isError && items.data?.length === 0 && (
        <p className="py-12 text-center text-sm text-muted-foreground">No market items match these filters.</p>
      )}
      {items.data && items.data.length > 0 && <div className="rounded-md border bg-card"><DataTable tableProps={{ reactTable: table }} /></div>}

      <Dialog open={!!editing} onOpenChange={(open) => { if (!open) { setEditing(null); setFormError(null); } }}>
        <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editing?.id ? "Edit market item" : "New market item"}</DialogTitle>
            <DialogDescription>Required fields and validation follow the admin market API.</DialogDescription>
          </DialogHeader>
          {editing && (
            <div className="space-y-4">
              <p className="rounded-md border bg-muted/40 p-3 text-sm text-muted-foreground">
                The app draws items from art bundled by assetKey. A new key shows a placeholder until the app ships its art.
              </p>
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="space-y-1"><Label>Slot *</Label><Select value={editing.slot} onValueChange={(value) => setEditing({ ...editing, slot: value as AdminMarketSlot })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="HAT">HAT</SelectItem><SelectItem value="EYEWEAR">EYEWEAR</SelectItem></SelectContent></Select></div>
                <div className="space-y-1"><Label>Rarity *</Label><Select value={editing.rarity} onValueChange={(value) => setEditing({ ...editing, rarity: value as AdminMarketRarity })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="COMMON">COMMON</SelectItem><SelectItem value="RARE">RARE</SelectItem><SelectItem value="EPIC">EPIC</SelectItem></SelectContent></Select></div>
                <div className="space-y-1"><Label>Name * (80 max)</Label><Input maxLength={80} value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} /></div>
                <div className="space-y-1"><Label>Name in Khmer * (120 max)</Label><Input maxLength={120} value={editing.nameKm} onChange={(event) => setEditing({ ...editing, nameKm: event.target.value })} /></div>
                <div className="space-y-1"><Label>Price *</Label><Input type="number" min={0} step={1} value={editing.price} onChange={(event) => setEditing({ ...editing, price: event.target.value })} /></div>
                <div className="space-y-1"><Label>Sort order *</Label><Input type="number" step={1} value={editing.sortOrder} onChange={(event) => setEditing({ ...editing, sortOrder: event.target.value })} /></div>
                <div className="space-y-1 sm:col-span-2"><Label>Asset key *</Label><Input maxLength={64} placeholder="e.g. blue_hat" value={editing.assetKey} onChange={(event) => setEditing({ ...editing, assetKey: event.target.value })} /><p className="text-xs text-muted-foreground">Use lowercase letters, digits, and underscores.</p></div>
                <div className="space-y-1"><Label>Species mask</Label><Input placeholder="crocodile, capybara" value={editing.speciesMask} onChange={(event) => setEditing({ ...editing, speciesMask: event.target.value })} /><p className="text-xs text-muted-foreground">Optional comma-separated species.</p></div>
                <div className="space-y-1"><Label>Minimum tier</Label><Select value={editing.minTier || "none"} onValueChange={(value) => setEditing({ ...editing, minTier: value === "none" ? "" : value })}><SelectTrigger><SelectValue placeholder="No minimum" /></SelectTrigger><SelectContent><SelectItem value="none">No minimum</SelectItem>{(tiers.data ?? []).map((tier) => <SelectItem key={tier.id} value={tier.tierName}>{tier.tierName}</SelectItem>)}</SelectContent></Select>{tiers.isError && <p className="text-xs text-destructive">Rank tiers could not be loaded; reload before setting a minimum tier.</p>}</div>
                <div className="space-y-1"><Label>Available from</Label><Input type="datetime-local" value={editing.availableFrom} onChange={(event) => setEditing({ ...editing, availableFrom: event.target.value })} /></div>
                <div className="space-y-1"><Label>Available until</Label><Input type="datetime-local" value={editing.availableUntil} onChange={(event) => setEditing({ ...editing, availableUntil: event.target.value })} /></div>
              </div>
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={editing.active} onChange={(event) => setEditing({ ...editing, active: event.target.checked })} />Active</label>
              {formError && <p className="text-sm text-destructive">{formError}</p>}
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>Cancel</Button>
            <Button disabled={save.isPending || tiers.isLoading || (!!editing?.minTier && tiers.isError)} onClick={submit}>{save.isPending ? "Saving…" : "Save item"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete “{deleteTarget?.name}”?</DialogTitle>
            <DialogDescription>
              {deleteTarget?.ownedCount
                ? `This item is owned by ${deleteTarget.ownedCount.toLocaleString()} user(s), so the backend will deactivate it and keep existing ownership.`
                : "No users own this item, so the backend will permanently delete it."}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
            <Button variant="destructive" disabled={remove.isPending} onClick={() => deleteTarget && remove.mutate(deleteTarget.id)}>
              {remove.isPending ? "Deleting…" : deleteTarget?.ownedCount ? "Deactivate item" : "Delete permanently"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
