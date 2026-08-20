import { Check, Copy } from "@phosphor-icons/react";
import { isValidElement, useState, type ComponentProps, type ReactElement, type ReactNode } from "react";
import ReactMarkdown, { type Components, type ExtraProps } from "react-markdown";
import remarkGfm from "remark-gfm";
import {
  CodeBody,
  CodeCopy,
  CodeHeader,
  CodeLine,
  CodeSurface,
  InlineCode,
  MarkdownBody,
  StreamingCaret
} from "./styles";
import type { CodeBlockProps, DiffLineTone, MessageContentProps } from "./types";

const lineTone = (line: string, language: string): DiffLineTone => {
  if (language !== "diff") return "context";
  if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@")) return "meta";
  if (line.startsWith("+")) return "added";
  if (line.startsWith("-")) return "removed";
  return "context";
};

function CodeBlock({ code, language }: CodeBlockProps): ReactElement {
  const [copied, setCopied] = useState<boolean>(false);
  const copy = (): void => {
    if (!navigator.clipboard) return;
    navigator.clipboard.writeText(code)
      .then((): void => setCopied(true))
      .catch((): void => undefined);
  };

  return (
    <CodeSurface aria-label={`${language} code block`}>
      <CodeHeader>
        <span>{language === "diff" ? "Changes" : language}</span>
        <CodeCopy type="button" onClick={copy} aria-label="Copy code block">
          {copied ? <Check size={12} weight="bold" /> : <Copy size={12} />}
          {copied ? "Copied" : "Copy"}
        </CodeCopy>
      </CodeHeader>
      <CodeBody>
        {code.split("\n").map((line: string, index: number) => (
          <CodeLine key={`${index}-${line}`} $tone={lineTone(line, language)}>{line || " "}</CodeLine>
        ))}
      </CodeBody>
    </CodeSurface>
  );
}

type MarkdownPreProps = ComponentProps<"pre"> & ExtraProps;

function MarkdownPre({ children }: MarkdownPreProps): ReactElement {
  let language = "text";
  let code = "";
  if (isValidElement<{ className?: string; children?: ReactNode }>(children)) {
    language = children.props.className?.replace("language-", "") || "text";
    code = String(children.props.children ?? "").replace(/\n$/, "");
  }
  return <CodeBlock code={code} language={language} />;
}

const markdownComponents: Components = {
  pre: MarkdownPre,
  code: ({ children, node: _node, ...props }): ReactElement => <InlineCode {...props}>{children}</InlineCode>,
  a: ({ children, node: _node, ...props }): ReactElement => (
    <a {...props} target="_blank" rel="noreferrer">{children}</a>
  )
};

export function MessageContent({ content, isStreaming, isUser }: MessageContentProps): ReactElement {
  return (
    <MarkdownBody $user={isUser}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents} skipHtml>
        {content}
      </ReactMarkdown>
      {isStreaming && <StreamingCaret aria-hidden>▌</StreamingCaret>}
    </MarkdownBody>
  );
}
