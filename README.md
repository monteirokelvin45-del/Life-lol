# Life OS — Hyper-Automated Personal Operating System (Android)

Projeto Android nativo completo em **Clean Architecture modularizada + MVVM + Unidirectional Data Flow (StateFlow)** e **UI 100% Jetpack Compose (OLED 120 FPS)**, unificando **Hábitos, Calistenia & Fisiologia, Estudos (Curva de Ebbinghaus), Leitura, Finanças Locais e Ingestão Documental por IA**.

---

## Arquitetura Multi-Módulo (`Clean Architecture`)

```text
Life-lol/
├── .github/workflows/
│   └── android-ci.yml                  # CI/CD: Compila Kotlin + KAPT + Testes + gera Debug APK
├── gradle/
│   ├── libs.versions.toml              # Version Catalog estritamente compatível (Kotlin 1.9.22 / Compose 1.5.8)
│   └── wrapper/gradle-wrapper.properties
├── core/
│   ├── database/                       # Room 2.6.1 + SQLCipher 4.5.4 AES-256 + Android Keystore GCM + WAL
│   │   └── src/main/kotlin/com/lifeos/core/database/
│   │       ├── LifeOsDatabase.kt
│   │       ├── converter/LifeOsTypeConverters.kt
│   │       ├── dao/LifeOsDao.kt
│   │       ├── di/DatabaseModule.kt
│   │       ├── entity/                 # 7 Entidades relacionais com FKs CASCADE e índices compostos
│   │       ├── model/RelationsAndProjections.kt
│   │       └── security/DatabasePassphraseProvider.kt
│   ├── domain/                         # Casos de Uso, Contratos e Matemática Comportamental
│   │   └── src/main/kotlin/com/lifeos/core/domain/
│   │       ├── algorithm/
│   │       │   ├── EbbinghausScheduler.kt         # Estágios [1, 3, 7, 16, 30] dias + R = e^(-t/S)
│   │       │   ├── BalanceWheelCalculator.kt      # LifeScore 0.0 a 10.0 ponderado
│   │       │   ├── StreakAndHeatmapCalculator.kt  # Matriz 7x20, Streaks bitmask e Avg. Log Time
│   │       │   └── TimeContextResolver.kt         # Mutação temporal (Manhã, Tarde, 17h-20h, Noite)
│   │       ├── model/DomainModels.kt
│   │       ├── repository/LifeOsRepository.kt
│   │       └── usecase/LifeOsUseCases.kt
│   ├── ai/                             # Cérebro de IA: Extração PDF/DOCX/MD + OCR ML Kit + JSON Schema
│   │   └── src/main/kotlin/com/lifeos/core/ai/
│   │       ├── di/AiModule.kt
│   │       ├── engine/StructuredPlanAiEngine.kt   # Ktor Structured JSON + Fallback PLN Offline
│   │       ├── engine/LocalRuleBasedPlanParser.kt
│   │       ├── extractor/DocumentTextExtractor.kt # PdfRenderer + ML Kit OCR + OpenXML DOCX + MD
│   │       └── schema/LifeOsExtractionSchema.kt   # JSON Schema Draft-07 estrito
│   ├── system/                         # Integrações de Baixo Nível com o Sistema Android
│   │   └── src/main/kotlin/com/lifeos/core/system/
│   │       ├── alarm/SystemAlarmScheduler.kt      # setExactAndAllowWhileIdle (Doze Mode)
│   │       ├── calendar/CalendarSyncManager.kt    # Sincronização nativa CalendarContract
│   │       ├── notification/LifeOsNotificationChannels.kt
│   │       ├── receiver/ExactAlarmReceiver.kt
│   │       └── service/ActiveExecutionService.kt  # Foreground Service (specialUse) Timer & Pomodoro
│   └── ui/                             # Design System OLED (#000000 / #0D0D0D) & Canvas 120 FPS
│       └── src/main/kotlin/com/lifeos/ui/
│           ├── components/
│           │   ├── BalanceWheel.kt                # Roda Polar Nightingale + Arcos Luminosos
│           │   ├── BentoCard.kt                   # Squircle OLED com borda sutil 1dp
│           │   ├── HabitMicroHeatmap.kt           # Matriz GPU 7x20 estilo HabitKit
│           │   ├── SmoothMetricSplineChart.kt     # Curva Bezier suave com gradiente
│           │   └── SpendingVisualizations.kt      # SegmentedCategoryBar & SpendingBurnMatrix
│           └── theme/LifeOsTheme.kt
├── app/                                # Aplicação Android Principal + Jetpack Glance Widget 4x2
│   └── src/main/
│       ├── AndroidManifest.xml         # Todas as permissões e serviços do Pilar 3
│       └── kotlin/com/lifeos/app/
│           ├── LifeOsApplication.kt
│           ├── MainActivity.kt
│           ├── data/LifeOsRepositoryImpl.kt
│           ├── di/RepositoryModule.kt
│           ├── features/
│           │   ├── home/               # Home Contextual Mutável + Roda da Vida + Ingestão IA
│           │   ├── habits/             # Micro-Heatmaps, Streaks, Notas e Métricas Detalhadas
│           │   ├── workout/            # Calistenia Semanal, Timer Foreground e Google Calendar
│           │   ├── study/              # Fila Ebbinghaus, Curva de Retenção e Leitura Ativa
│           │   ├── finance/            # Carteira OLED, Burn Matrix e Meios Locais (M-Pesa/e-Mola)
│           │   └── widget/             # LifeOsGlanceWidget 4x2 (Jetpack Glance)
│           └── ui/LifeOsAppShell.kt
├── docs/
│   ├── ARCHITECTURE_9_PILARES.md
│   └── references/                     # 10 referências visuais (01..10.jpg)
└── tests/                              # Validação automatizada de integridade, DDL SQLite e Queries
```

---

## Jornada Prática de 24 Horas (Atrito Zero)

- **✨ O "Portal Mágico" de IA (Onboarding em <5s):** Recebe arquivos `.pdf`, `.docx`, `.md`, fotos ou textos compartilhados direto do **WhatsApp (`ACTION_SEND`)** ou ditados por **Voz**, estrutura Hábitos, Treinos, Estudos e Finanças em JSON estrito e configura tabelas, alarmes e Google Calendar em 1 toque (**"Confirmar Sistema"**).
- **🌅 07:00 — Despertar, Ebbinghaus & Leitura Ativa:** Alarme nativo `setExactAndAllowWhileIdle` das revisões `[1, 3, 7, 16, 30 dias]`, **Modo Leitura Ativa** com cronômetro ao vivo (`páginas/hora` + previsão do dia da semana de conclusão) e **Scanner OCR pela Câmera** para salvar citações de livros físicos em `book_quotes`.
- **☀️ 14:00 — Finanças em 2 Segundos & Inteligência Cruzada:** Barra relâmpago por texto ou voz (*"Almoço 38 débito"* → categorizado automaticamente em *Alimentação*), cruzamento comportamental real (*Treino × Gastos em Alimentação*) e serviço **Pomodoro 25m/50m** na tela de bloqueio.
- **🏋️ 18:00 — Fisiologia & Descanso Tátil:** Mutação automática da Home para o treino do dia (séries, repetições e `lastWeightKg`), cronômetro Foreground de 90s na tela bloqueada com **vibração tátil dupla + bip discreto** ao zerar e bloqueio automático no Google Calendar.
- **🌙 22:00 — Fechamento da Roda da Vida & Widget Interativo:** Auditoria noturna da **Roda do Equilíbrio (0–10)** com celebração de *Streak* e **Widget Glance 4x2** na tela inicial do Android com bolinhas `S..D` e check-in direto (`ActionCallback`) sem abrir o app.

---

## Como Compilar e Executar

### 1. Android Studio (Hedgehog / Iguana / Jellyfish ou superior)
1. Clone o repositório e abra a pasta raiz `Life-lol` no **Android Studio**.
2. Use **JDK 17** em *Settings → Build, Execution, Deployment → Build Tools → Gradle*.
3. Sincronize o Gradle e execute a configuração **`app`** em um dispositivo físico ou emulador Android 8.0+ (`minSdk = 26`, `targetSdk = 34`).

### 2. GitHub Actions (Build Automático de APK na Nuvem)
Todo *push* dispara o workflow [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml), executando os testes unitários e gerando o artefato instalável **`life-os-debug-apk` (`app-debug.apk`)**.

### 3. Suíte de Auditoria de Schema SQLite & Integridade Estrutural
```bash
pytest -v tests/
```
