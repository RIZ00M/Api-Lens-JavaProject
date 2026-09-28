export interface DiscoveryAttemptSummaryLite {
  url: string;
  discoveryMethod: string;
  httpStatus: number | null;
  success: boolean;
  errorMessage: string | null;
}

export interface DiscoveryResultResponse {
  specificationFound: boolean;
  sourceUrl: string | null;
  specificationFormat: string | null;
  confidence: "HIGH" | "MEDIUM" | "LOW" | null;
  contractId: string | null;
  contractParsed: boolean;
  parseMessages: string[];
  attempts: DiscoveryAttemptSummaryLite[];
}

export interface DiscoveryAttempt {
  id: string;
  url: string;
  discoveryMethod: string;
  httpStatus: number | null;
  contentType: string | null;
  responseSizeBytes: number;
  success: boolean;
  errorMessage: string | null;
  confidence: "HIGH" | "MEDIUM" | "LOW" | null;
  detectedFormat: string | null;
  attemptedAt: string;
}
