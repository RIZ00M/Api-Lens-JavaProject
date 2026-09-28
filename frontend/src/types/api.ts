export interface Api {
  id: string;
  name: string;
  baseUrl: string;
  openApiUrl: string | null;
  documentationUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface NewApiInput {
  name: string;
  baseUrl: string;
  openApiUrl?: string;
  documentationUrl?: string;
}

export interface ApiErrorPayload {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
