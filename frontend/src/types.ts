export interface ConnectionStatus {
  isConnected: boolean;
}

export enum DataType {
  JSON = 'JSON',
  DEFAULT_CSV = 'DEFAULT_CSV',
  MONGODB_CSV = 'MONGODB_CSV',
  MONGODB_TSV = 'MONGODB_TSV',
  EXCEL = 'EXCEL',
  INFORMIX_UNLOAD = 'INFORMIX_UNLOAD',
  INFORMIX_UNLOAD_CSV = 'INFORMIX_UNLOAD_CSV',
  TDF = 'TDF',
  MYSQL = 'MYSQL',
  ORACLE = 'ORACLE',
  POSTGRESQL_CSV = 'POSTGRESQL_CSV',
  POSTGRESQL_TEXT = 'POSTGRESQL_TEXT',
  RFC4180 = 'RFC4180',
  AUTODETECT = 'AUTODETECT',
}

export interface Mapping {
  name: string;
  path: string;
  transformations: string[];
}

export interface UnfinishedUpload {
  fileName: string;
  uploadId: string;
  dataType: DataType;
  mappings: Mapping[];
  error: string;
  isComplete: boolean;
  processed: number;
  outOf: number;
}

export enum FilterType {
  STARTS_WITH = 'STARTS_WITH',
  ENDS_WITH = 'ENDS_WITH',
  CONTAINS = 'CONTAINS',
  EQUALS = 'EQUALS',
  NONE = 'NONE',
}

export enum EnrichmentMethod {
  SHALLOW = 'SHALLOW',
  DEEP = 'DEEP',
  NONE = 'NONE',
}

export interface SearchParams {
  recordType?: string;
  filter: string;
  filterType: FilterType;
  enrichmentMethod: EnrichmentMethod;
  joinOn: string;
  maxDepth: number;
  skip: number;
  limit: number;
}
