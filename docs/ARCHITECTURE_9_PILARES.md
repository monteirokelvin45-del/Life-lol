# Life OS — Arquitetura dos 9 Pilares & Mapeamento de Referências Visuais

Este documento consolida na base de versão do repositório os **9 Pilares Estruturais** inegociáveis do **Life OS** e o mapeamento técnico das **10 Referências Visuais (`docs/references/01..10`)** para os pacotes Android.

---

## Resumo dos 9 Pilares Estruturais

1. **Pilar 1 — Visão de Produto & Ciência Comportamental:** Fricção zero via ingestão de arquivos caóticos (`.pdf`, `.docx`, `.md`), interface contextual mutável por faixa horária (*Manhã 06h–12h Foco/Estudos*, *Fim de Tarde 17h–20h Fisiologia/Treino*, *Noite 20h–23h Fechamento Financeiro/Sono*) e gamificação visual baseada em aversão à perda (Streaks + Micro-Heatmaps).
2. **Pilar 2 — Arquitetura Técnica Android:** Clean Architecture modularizada + MVVM + Unidirectional Data Flow (`StateFlow<ScreenUiState>` + `sealed interface Intent`), 100% Jetpack Compose (BOM `2024.02.00`, Kotlin `1.9.22`, Compose Compiler `1.5.8`, Hilt `2.50`, Room `2.6.1`, SQLCipher `4.5.4`, Ktor `2.3.8`, Glance `1.0.0`, WorkManager `2.9.0`, ML Kit `19.0.0`).
3. **Pilar 3 — Permissões & Manifesto do Sistema:** `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_SPECIAL_USE`, `READ_CALENDAR` / `WRITE_CALENDAR`, `CAMERA`, `READ_MEDIA_DOCUMENTS`, `ExactAlarmReceiver` e `ActiveExecutionService`.
4. **Pilar 4 — Persistência Local Relacional (`:core:database`):** Room + SQLCipher AES-256 com envelope criptográfico no Android Keystore (`AES/GCM/NoPadding`), modo WAL e consultas reativas em `Flow`.
5. **Pilar 5 — Cérebro de IA (Document Engine & JSON Schema):** Extração textual de PDF/DOCX/MD para JSON Schema estrito (`detected_habits`, `detected_workouts`, `detected_studies`, `detected_finances`) persistido em transação atômica (`@Transaction`).
6. **Pilar 5 — Ciência Comportamental, Retenção & Gamificação:**
   - **Roda do Equilíbrio Cósmico (`BalanceWheel`):** Score `0.0–10.0` ponderando Corpo (30%), Foco (25%), Finanças (25%) e Consistência (20%) com preenchimento elástico (`spring()`).
   - **Motor Circadiano & Janelas Biológicas (`BehavioralRetentionEngine`):** Linha cronobiológica com Pico Intelectual (`08:30–11:30`), Corte Exato de Cafeína (*Caffeine Cutoff* `14:00`) e Pico Neuromuscular (`17:00–19:30`).
   - **Escudo Anti-Falha (`StreakFreezeShieldBanner`):** Acúmulo de créditos de repouso por consistência (+1 a cada 7 check-ins limpos) protegendo Streaks e reprogramando Ebbinghaus (+1 dia) sem punição desmotivadora.
   - **Ghost Multiplayer Assíncrono (`GhostSquadSilentAuraRow`):** Esquadrão silencioso com auras de foco em tempo real de até 3 amigos, sem salas de conversa ou feeds de distração.
   - **Contratos de Compromisso Anti-Charity (`AntiCharityCommitmentBanner`):** Caução financeira semanal configurável com cláusula de aversão à perda vinculada a instituição beneficente.
7. **Pilar 6 — A Suíte Completa de 14 Gráficos em Canvas (120 FPS Nativos):**
   1. `HabitMicroHeatmap` & `AnnualGitHubHeatmap7x52` — Micro-Heatmap `7x52` estilo GitHub.
   2. `BalanceWheel` — Roda do Equilíbrio Cósmico com arcos concêntricos elásticos.
   3. `SpendingBurnMatrix` — Escada de Queima de Caixa em tijolos digitais degradê laranja → vermelho carmesim.
   4. `SmoothMetricSplineChart` — Curva Spline de Bézier com **Haptic Scrubber** milimétrico.
   5. `CompoundEffectFlameChart` — Curva do Efeito Composto (Dia 1, 7, 30 e 100) com Chama 3D vetorial.
   6. `ConcentricFocusMacroRings` — Anéis Concêntricos de Foco e Macros estilo Apple Fitness.
   7. `ProgressiveOverloadRegressionChart` — Barras emparelhadas + Linha de Regressão Linear.
   8. `FluidSankeyCashflowChart` — Diagrama de Fluxo de Sankey Fluido (Investimentos, Fixos, Vazamentos).
   9. `EbbinghausSawtoothCurveChart` — Curva Dente-de-Serra exata de Ebbinghaus com saltos a 100%.
   10. `MultivariateCorrelationScatterChart` — Matriz de Correlação Multivariada da IA (Sono/Treino × Impulso).
   11. `AnatomicalMuscleFatigueMap` — Mapa Anatômico Anterior & Posterior de Fadiga Muscular.
   12. `ChronoNightingaleWheel24h` — Chrono Wheel de 24 Horas (Rosa de Nightingale).
   13. `KnowledgeBurndownConeChart` — Burndown de Conhecimento com Cone de Incerteza (`p10/p50/p90`).
   14. `HexagonalRpgRadarChart` — Radar Hexagonal de Atributos RPG (6 vértices).
8. **Pilar 7 — Segurança Local-First & Performance Zero Garbage Collection:**
   - **Dark Mode OLED Absoluto (`#000000`):** Corte físico de emissão luminosa em painéis AMOLED/OLED.
   - **Persistência Criptografada (`LifeOsDatabase` + `DatabasePassphraseProvider`):** Room + SQLCipher AES-256 com chave envelopada em `AES/GCM/NoPadding` no Android Keystore.
   - **Performance Zero Garbage Collection:** Todos os 14 gráficos utilizam `Modifier.drawWithCache` e objetos `Path` pré-alocados e reaproveitados (`reset()`), zero alocação de `Path()` em `onDrawBehind` a 120 FPS.
9. **Pilar 9 — Execução Modular por Fases:**
   - **Fase 1 (Concluída):** `:core:database` — Entidades, Relacionamentos, `LifeOsTypeConverters`, `LifeOsDao`, `DatabasePassphraseProvider`, `LifeOsDatabase` (SQLCipher) e `DatabaseModule` (Hilt).
   - **Fase 2 (Próxima):** `:core:domain` & `:core:ai` — Casos de Uso, `EbbinghausScheduler`, `BalanceWheelCalculator`, `DocumentTextExtractor` e `IngestPlanUseCase`.
   - **Fase 3:** `:core:system` & `:features:widget` — `SystemAlarmScheduler`, `ExactAlarmReceiver`, `ActiveExecutionService`, `CalendarSyncManager` e `LifeOsGlanceWidget`.
   - **Fase 4:** `:core:ui` & `:app` — Design System OLED, `HabitMicroHeatmap`, `BalanceWheel`, Home Contextual por Horário e fluxos completos em Jetpack Compose.

---

## Mapeamento das 10 Referências Visuais (`docs/references/`)

| Arquivo | Referência Visual | Impacto Direto na Modelagem & UI |
| :--- | :--- | :--- |
| `01-bento-wallet-home.jpg` | Bento Grid OLED + Wallet + Barra Segmentada de Gastos | Suportado por `observeCashflowSummary` e `observeCategoryExpenses` em `LifeOsDao`. |
| `02-sleep-analytics-spline.jpg` | Curva Bezier suave com preenchimento gradiente + comparativo | Base estética para curvas temporais de Sono/Fisiologia e evolução semanal. |
| `03-spending-pixel-burn-matrix.jpg` | Matriz cumulativa de blocos (`$123 today`, `May` vs `Apr`) | Suportado por `observeDailyExpenseBurn` agrupado por `epoch_day`. |
| `04-balance-wheel-polar-radar.jpg` | Roda do Equilíbrio Polar (Setores radiais proporcionais + `8.7/10`) | Alimentada pelos 4 scores ponderados do Pilar 6.2 + cards de resumo por domínio. |
| `05-habit-detail-heatmap-stats.jpg` | Detalhe de Hábito (`Streak`, `Total Check-Ins`, `Avg. Log Time`, `Goal %`) | Suportado por `logged_at_millis`, `created_at_epoch_day` e `daily_goal_value`. |
| `06-streak-ignition-dopamine-loop.jpg` | Tela de celebração de Streak com chama radiante e barra semanal | Alimentada por `current_streak`, `longest_streak` e `target_days_mask`. |
| `07-glance-widget-streak-steps.jpg` | Widget Squircle `#0D0D0D` (`32 DAYS` + checkmarks `Mon..Sun` + barra) | Suportado diretamente por `LifeOsGlanceWidget` consumindo `LifeOsDao`. |
| `08-nutrition-calories-macros-weight.jpg` | Cards de progresso fisiológico, metas diárias e anel de proporção | Tokens de progresso semanais/mensais e barras verticais em gradiente. |
| `09-habitkit-micro-heatmaps-calendar.jpg` | Lista de Hábitos com Micro-Heatmaps multi-cor + Calendário mensal | Suportado por `HabitWithLogs`, `hex_color`, `icon_key` e `description`. |
| `10-habit-daily-goal-notes.jpg` | Card de Hábito com botão `Done`, `Add Note` e métricas rápidas | Suportado pela coluna `note` e `value_logged` em `HabitLogEntity`. |
