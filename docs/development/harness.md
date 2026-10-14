# cursor-harness v3.2.1

Проект подключён к [nirmalarya/cursor-harness](https://github.com/nirmalarya/cursor-harness) **v3.2.1** в режиме **enhance** (существующее приложение).

## Почему не harness/harness-skills

`harness/harness-skills` — навыки для платформы **Harness CI/CD** (пайплайны, MCP `harness-mcp-v2`, API-ключ Harness). ZeroData Chat собирается GitHub Actions (`.github/workflows/build.yml`) и не использует аккаунт Harness. Для автономной доработки кода нужен именно cursor-harness.

## Установка CLI

```bash
./scripts/install-harness.sh
# или
pip install -r requirements-harness.txt
export PATH="$HOME/.local/bin:$PATH"
cursor-harness --version   # cursor-harness v3.2.1
```

Пакет не опубликован на PyPI, пин идёт на git-тег `v3.2.1`.

## Запуск сессий

Инициализатор уже выполнен. Спека — блэкаут-MVP (короткий MQTT-текст, не клон Telegram): `docs/development/enhancement_spec.txt`.
Не вызывайте `greenfield`. Первая задача в очереди — FAB чата по ID.

```bash
cursor-harness enhance . --spec docs/development/enhancement_spec.txt
```

**Windows (cmd):** Python 3.11 по умолчанию пишет файлы в cp1251, и harness 3.2.1 падает на emoji в хуках (`charmap codec can't encode`). Запускать из корня git-клона (там лежат `docs\development\enhancement_spec.txt` и `.git`):

```bat
set PYTHONUTF8=1
set PYTHONIOENCODING=utf-8
python -m cursor_harness.cli enhance . --spec docs\development\enhancement_spec.txt
```

Или после `git pull`: `scripts\enhance.cmd`.

**Windows, промпты не в пакете:** wheel 3.2.1 без `prompts/*.md` → `FileNotFoundError: enhancement_coding.md`. Сначала `git pull`, затем `python scripts\repair-harness-prompts.py` (это уже внутри `scripts\enhance.cmd`). Без pull можно вручную скопировать `.md` из https://github.com/nirmalarya/cursor-harness/tree/v3.2.1/cursor_harness/prompts в `%LOCALAPPDATA%\Programs\Python\Python311\Lib\site-packages\cursor_harness\prompts\`.

Нужны Cursor login / `cursor-agent`. CLI сам ставит `cursor-agent` (фича 3.2.1). Puppeteer MCP **не** добавлялся: UI нативный, не браузерный. Проверка — `./gradlew test` и при наличии устройства `./gradlew connectedAndroidTest`.

## Артефакты

| Файл | Роль |
|------|------|
| `docs/development/enhancement_spec.txt` | Спека для enhance |
| `enhancement_list.json` | Очередь enhance-промптов (`completed`) |
| `feature_list.json` | Та же очередь для `core.py` (`passes`) |
| `cursor-progress.txt` | Заметки сессий |
| `init.sh` | `./gradlew test` |
| `.cursor/hooks.json` | Хуки cursor-agent |
| `.cursor/rules/` | Правила архитектуры и harness |
| `.cursorignore` | Блок секретов для агента |
| `hooks/` | `block-secrets.sh`, `git-workflow.sh`, `gradle-test.sh` |

Два JSON нужны из-за расхождения upstream: промпты enhance читают `enhancement_list.json`, цикл завершения — `feature_list.json`. Помечайте оба файла вместе.

## Что не запускалось

Автономный цикл кодирования (`Sessions 2–N`) не стартовал: он требует интерактивный `cursor-agent login` и начал бы сам править продукт. Очередь готова, можно запускать enhance локально.
