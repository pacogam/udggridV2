import { MarkdownDirective } from "lit-markdown";
import { directive } from "lit/directive.js";
import { marked } from "marked";

type Options = typeof MarkdownDirective.defaultOptions;

export class OgMarkdownDirective extends MarkdownDirective {

    render(rawMarkdown: string, options?: Partial<Options>) {
        marked.use({
            gfm: true
        });
        return super.render(rawMarkdown, options) as any;
    }
}

// @ts-ignore
export const resolveOgMarkdown = directive(OgMarkdownDirective);