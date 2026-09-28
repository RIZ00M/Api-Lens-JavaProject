export interface ContractSummary {
  id: string;
  specificationVersion: string | null;
  title: string | null;
  endpointCount: number;
  schemaCount: number;
  discoveredAt: string;
}

export interface ServerView {
  url: string;
  description: string | null;
}

export interface SecuritySchemeView {
  name: string;
  type: string | null;
  scheme: string | null;
  bearerFormat: string | null;
  inLocation: string | null;
  keyName: string | null;
  description: string | null;
}

export interface SchemaView {
  name: string;
  definition: unknown;
}

export interface ParameterView {
  name: string;
  location: string;
  required: boolean;
  type: string | null;
  format: string | null;
  description: string | null;
  defaultValue: string | null;
  enumValues: string[];
}

export interface RequestBodyView {
  required: boolean;
  content: Record<string, unknown> | null;
}

export interface ResponseView {
  statusCode: string;
  description: string | null;
  content: Record<string, unknown> | null;
}

export interface EndpointView {
  id: string;
  path: string;
  method: string;
  operationId: string | null;
  summary: string | null;
  description: string | null;
  deprecated: boolean;
  tags: string[];
  requiredSecuritySchemeNames: string[];
  parameters: ParameterView[];
  requestBody: RequestBodyView | null;
  responses: ResponseView[];
}

export interface ContractDetail {
  id: string;
  specificationVersion: string | null;
  title: string | null;
  description: string | null;
  baseUrl: string | null;
  sourceUrl: string;
  discoveredAt: string;
  servers: ServerView[];
  securitySchemes: SecuritySchemeView[];
  schemas: SchemaView[];
  endpoints: EndpointView[];
}
