export const MAX_KNOWLEDGE_FILE_SIZE = 50 * 1024 * 1024;

export const ALLOWED_KNOWLEDGE_FILE_EXTENSIONS = [
  '.pdf',
  '.docx',
  '.doc',
  '.txt',
  '.md',
] as const;

export const KNOWLEDGE_FILE_ACCEPT = ALLOWED_KNOWLEDGE_FILE_EXTENSIONS.join(',');

export const KNOWLEDGE_FILE_TYPES_LABEL = 'PDF、DOCX、DOC、TXT、MD';
