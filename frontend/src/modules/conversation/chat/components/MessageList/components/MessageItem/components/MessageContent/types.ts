export type MessageContentProps = {
  content: string;
  isStreaming: boolean;
  isUser: boolean;
};

export type CodeBlockProps = {
  code: string;
  language: string;
};

export type DiffLineTone = "added" | "removed" | "meta" | "context";
