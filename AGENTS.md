# Release Notes formatting rules

When creating GitHub releases, creating changelogs, or generating any kind of release notes for the EzClient project, please follow these rules strictly:

1. **Clean formatting**: Ensure all descriptions and text are formatted cleanly. Do not use raw escape sequences like `\n` or `\r` directly in the markdown or text output unless writing code snippets.
2. **Proper Markdown**: Use proper Markdown syntax for lists, headings, and line breaks instead of relying on explicit newline characters inside JSON strings when outputting raw text.
3. **No messy dumps**: Ensure that script outputs or CLI outputs don't dump unformatted text or raw JSON fields into the release body if they are meant to be human-readable. Use `replace('\\n', '\n')` or similar in your scripts to format the output correctly before pushing to GitHub APIs.
4. **Professional tone**: Keep release notes clear, professional, and user-friendly.

Follow these rules for all future version releases or hotfixes!
