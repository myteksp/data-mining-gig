enum MappingType {
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
    AUTODETECT = 'AUTODETECT'
}
interface Mapping {
    "name": string,
    "path": string,
    "transformations": string[]
}
interface UnfinishedUpload {
    "fileName": string,
    "uploadId": string,
    "dataType": MappingType,
    "mappings": Mapping[]
    "error": string,
    "isComplete": boolean,
    "processed": number,
    "outOf": number
}
