# FLE-56 ES/EN/PT Internationalization

## User Story

As a parent in a Spanish, English or Portuguese market, I want the Fledge UI copy to be available in my language so the family onboarding and money workflows feel local and trustworthy.

## Acceptance Criteria

### AC-01 English Resources Are Complete

Given the default Compose resources contain a translatable string
When the English locale is built
Then the English resource file includes that string key.

### AC-02 Portuguese Resources Are Complete

Given the default Compose resources contain a translatable string
When the Portuguese locale is built
Then the Portuguese resource file includes that string key.

### AC-03 Placeholders Are Compatible Across Locales

Given a resource string uses dynamic values or literal percent symbols
When it is translated to English or Portuguese
Then the translation keeps positional placeholders compatible with the default resource and escapes literal percent symbols.

### AC-04 UI Design Gate

Given this task only adds translations and resource validation
When the SDD design check is evaluated
Then Pencil is marked N/A because no new screen, component or layout state is introduced.

## Notes

- `values/` remains the default resource set already used by the app.
- `values-en/` is kept complete for English.
- `values-pt/` is added for Portuguese-speaking markets.
- User-facing resource placeholders must use positional format such as `%1$s`, `%1$d` and `%%`.
