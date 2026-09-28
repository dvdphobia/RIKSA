import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  type ColumnDef,
  getCoreRowModel,
  type PaginationState,
  useReactTable,
} from "@tanstack/react-table";
import { useEffect, useState } from "react";
import { apiFetch } from "./api";
import type { SpringPage } from "./types";

type Params = Record<string, string | number | boolean | undefined | null>;

interface ServerTableOptions<T> {
  /** Query-key root, e.g. "users". Invalidating `[resource]` refetches the table. */
  resource: string;
  /** API path; defaults to `/admin/${resource}`. */
  path?: string;
  columns: ColumnDef<T>[];
  filters?: Params;
  pageSize?: number;
}

/**
 * Server-paginated table backed by React Query. Accepts Spring pages
 * (`{ content, totalElements }`) and plain arrays (unpaginated endpoints).
 */
export function useServerTable<T>({
  resource,
  path = `/admin/${resource}`,
  columns,
  filters = {},
  pageSize = 10,
}: ServerTableOptions<T>) {
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize,
  });

  // Jump back to the first page whenever a filter changes.
  const filterKey = JSON.stringify(filters);
  useEffect(() => {
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  }, [filterKey]);

  const query = useQuery({
    queryKey: [resource, "list", path, filters, pagination],
    queryFn: async () => {
      const data = await apiFetch<SpringPage<T> | T[]>(path, {
        params: {
          ...filters,
          page: pagination.pageIndex,
          size: pagination.pageSize,
        },
      });
      return Array.isArray(data)
        ? { rows: data, total: data.length }
        : { rows: data.content, total: data.totalElements };
    },
    placeholderData: keepPreviousData,
  });

  const total = query.data?.total ?? 0;
  const reactTable = useReactTable<T>({
    data: query.data?.rows ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: Math.max(1, Math.ceil(total / pagination.pageSize)),
    state: { pagination },
    onPaginationChange: setPagination,
  });

  return { reactTable, query };
}
