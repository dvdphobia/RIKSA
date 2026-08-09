import type {
  BaseRecord,
  CreateParams,
  CreateResponse,
  DataProvider,
  DeleteOneParams,
  DeleteOneResponse,
  GetListParams,
  GetListResponse,
  GetOneParams,
  GetOneResponse,
  UpdateParams,
  UpdateResponse,
} from "@refinedev/core";
import { apiFetch } from "../lib/api";
import type { SpringPage } from "../lib/types";

const buildFilters = (filters?: { field: string; value: unknown }[]) => {
  const params: Record<string, string> = {};
  for (const f of filters ?? []) {
    if (f.value === undefined || f.value === null || f.value === "") continue;
    params[f.field] = String(f.value);
  }
  return params;
};

export const dataProvider: DataProvider = {
  getList: async <TData extends BaseRecord = BaseRecord>({
    resource,
    pagination,
    filters,
  }: GetListParams): Promise<GetListResponse<TData>> => {
    const currentPage = pagination?.currentPage ?? 1;
    const pageSize = pagination?.pageSize ?? 20;
    const params = buildFilters(
      filters as { field: string; value: unknown }[] | undefined,
    );

    if (resource === "categories") {
      const data = await apiFetch<TData[]>("/admin/categories");
      return { data, total: data.length };
    }

    if (resource === "blog") {
      const data = await apiFetch<{ content: TData[]; totalElements: number }>(
        "/admin/blog",
        { params: { ...params, page: currentPage - 1, size: pageSize } },
      );
      return { data: data.content, total: data.totalElements };
    }

    const data = await apiFetch<SpringPage<TData>>(`/admin/${resource}`, {
      params: { ...params, page: currentPage - 1, size: pageSize },
    });
    return { data: data.content, total: data.totalElements };
  },

  getOne: async <TData extends BaseRecord = BaseRecord>({
    resource,
    id,
  }: GetOneParams): Promise<GetOneResponse<TData>> => {
    const data = await apiFetch<TData>(`/admin/${resource}/${id}`);
    return { data };
  },

  create: async <TData extends BaseRecord = BaseRecord, TVariables = unknown>({
    resource,
    variables,
  }: CreateParams<TVariables>): Promise<CreateResponse<TData>> => {
    const data = await apiFetch<TData>(`/admin/${resource}`, {
      method: "POST",
      body: variables,
    });
    return { data };
  },

  update: async <TData extends BaseRecord = BaseRecord, TVariables = unknown>({
    resource,
    id,
    variables,
  }: UpdateParams<TVariables>): Promise<UpdateResponse<TData>> => {
    const data = await apiFetch<TData>(`/admin/${resource}/${id}`, {
      method: "PUT",
      body: variables,
    });
    return { data };
  },

  deleteOne: async <TData extends BaseRecord = BaseRecord, TVariables = {}>({
    resource,
    id,
  }: DeleteOneParams<TVariables>): Promise<DeleteOneResponse<TData>> => {
    await apiFetch<void>(`/admin/${resource}/${id}`, { method: "DELETE" });
    return { data: {} as TData };
  },

  getApiUrl: () => "",
};
