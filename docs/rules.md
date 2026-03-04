# Rule Engine — User-Defined Automation (`rule-service`)

The rule engine enables users to create automation rules that react to device state changes, time-of-day conditions, and other events. Rules follow a trigger → condition → action model, with support for stateful actions (set-and-restore), time filters, and templated variables. Scenes provide a simpler one-shot action execution model.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Rule Execution Model](#rule-execution-model)
- [Condition and Trigger System](#condition-and-trigger-system)
- [Action Framework](#action-framework)
- [Rule Templates and Catalog](#rule-templates-and-catalog)
- [Variable Resolution and Templating](#variable-resolution-and-templating)
- [Rule Context](#rule-context)
- [Rule Lifecycle](#rule-lifecycle)
- [Scene Framework](#scene-framework)
- [Execution Threading Model](#execution-threading-model)
- [Persistence](#persistence)
- [Rule Events](#rule-events)
- [Key Files](#key-files)

---

## Architecture Overview

```
User creates rule from template
        │
        ▼
┌─────────────────┐
│  Rule Catalog    │  XML templates → ConditionConfig + ActionConfig
│  (templates)     │
└────────┬────────┘
         │ instantiate
         ▼
┌─────────────────┐     ┌─────────────────┐
│ RuleDefinition   │────▶│  Cassandra      │  Persisted configs + variables
│ (configs+vars)   │     │  (rule table)   │
└────────┬────────┘     └─────────────────┘
         │ create
         ▼
┌─────────────────┐
│  SimpleRule      │  Condition + StatefulAction + RuleContext
│  (runtime)       │
└────────┬────────┘
         │ execute
         ▼
┌─────────────────────────────────────────┐
│  PlaceExecutorEventLoop                  │
│  (single-threaded per place)             │
│                                          │
│  RuleHandler ──▶ Rule.execute(event)     │
│  RuleHandler ──▶ Rule.execute(event)     │
│  SceneHandler ──▶ Scene.fire()           │
└──────────────────────────────────────────┘
```

**Service:** `platform/arcus-containers/rule-service/`
**Core rule engine:** `platform/arcus-rules/`
**Rule library:** `platform/arcus-lib/` (definitions, catalog, DAOs)
**Capability definitions:** `common/arcus-model/src/main/resources/capability/rule.xml`, `ruletemplate.xml`

---

## Rule Execution Model

### Rule Interface

```java
interface Rule {
    RuleContext getContext();
    boolean isSatisfiable();      // Can this rule possibly fire?
    void activate();              // Initialize for execution
    void execute(RuleEvent event); // Evaluate and potentially fire
    void deactivate();            // Stop execution, clean up
}
```

### SimpleRule

`SimpleRule` is the concrete implementation combining a `Condition`, a `StatefulAction`, and a `RuleContext`:

1. **Satisfiability check** — both condition and action must be satisfiable
2. **Activation** — activates condition (may schedule events) and action (prepares resources)
3. **Execution** — on each event:
   - If already firing (stateful action in progress): call `action.keepFiring(event)`
   - Otherwise: evaluate `condition.shouldFire(event)`
   - If condition fires: execute action, track `ActionState`
   - Track firing state in `_firing` context variable
4. **Deactivation** — deactivates condition and action, clears state

### Event Flow

```
Event arrives (e.g., AttributeValueChanged)
  → PlaceExecutorEventLoop routes to RuleHandler
  → RuleHandler checks service level (premium rules need premium account)
  → RuleHandler updates satisfiability if changed
  → Rule.execute(event)
    → Condition.shouldFire(context, event)?
    → Action.execute(context)
  → RuleHandler syncs dirty variables to database
```

---

## Condition and Trigger System

### Condition Interface

```java
interface Condition {
    boolean isSatisfiable(ConditionContext ctx);
    boolean handlesEventsOfType(RuleEventType type);  // Optimization
    void activate(ConditionContext ctx);
    void deactivate(ConditionContext ctx);
    boolean shouldFire(ConditionContext ctx, RuleEvent event);
    boolean isSimpleTrigger();  // Stateless?
}
```

### Trigger Types

**SimpleTrigger** — Stateless triggers that fire on matching events:

| Trigger | Description |
|---------|-------------|
| `ValueChangeTrigger` | Fires on attribute value change matching old/new constraints |
| `QueryChangeTrigger` | Fires when a query predicate transitions from false → true |
| `ReceivedMessageTrigger` | Fires on specific message type to specific address |
| `ThresholdTrigger` | Fires when value crosses a threshold |

**Stateful Conditions** — Maintain internal state machines:

| Condition | Description |
|-----------|-------------|
| `DurationTrigger` | Fires when a matcher matches continuously for a specified duration. States: `InactiveState` → `TriggeredState` (schedules wake-up event) |
| `StatefulCondition` | Base class with `State` implementations that have `transitionsOnEventOfType()` and `isFiring()` methods |

### Filters

Filters wrap conditions to add time-based constraints:

| Filter | Description |
|--------|-------------|
| `TimeOfDayFilter` | `before(time)`, `after(time)`, `between(start, end)` |
| `DayOfWeekFilter` | `on(dayOfWeek...)` |
| `DurationFilter` | Time window filtering |

### Context Matchers

`ContextMatcher` matches models against predicates in triggers:

```java
interface ContextMatcher {
    boolean isSatisfiable(ConditionContext ctx);
    boolean matches(ConditionContext ctx, Model model);
    Set<RuleEventType> reevaluateOnEventsOfType();
}
```

`ModelPredicateMatcher` implements this to filter which device models can trigger a rule.

### Condition Config Types

| Type | Config Class | Generates |
|------|-------------|-----------|
| `value-change` | `ValueChangeConfig` | `ValueChangeTrigger` |
| `duration` | `DurationConfig` | `DurationTrigger` |
| `time-of-day` | `TimeOfDayConfig` | `TimeOfDayFilter` |
| `received-message` | `ReceivedMessageConfig` | `ReceivedMessageTrigger` |
| `or` | `OrConfig` | `OrCondition` (combines multiple) |

---

## Action Framework

### Action Interface

```java
interface Action {
    String getName();
    String getDescription();
    void execute(ActionContext ctx);
}
```

### Stateful Actions

`StatefulAction` extends actions with lifecycle and continuation:

```java
interface StatefulAction {
    boolean isSatisfiable(ActionContext ctx);
    void activate(ActionContext ctx);
    ActionState execute(ActionContext ctx);  // Returns INACTIVE, FIRING, or COMPLETE
    ActionState keepFiring(ActionContext ctx, RuleEvent event);
    void deactivate(ActionContext ctx);
}
```

### Action Implementations

| Action | Description |
|--------|-------------|
| `SendAction` | Sends a message to a device address. Attributes can be static or templated from context variables. |
| `SetAndRestore` | Sets an attribute value, then restores it after a duration. States: `INACTIVE` → `SETTING` → `RESTORING` → `COMPLETE` |
| `DelayAction` | Waits a specified duration before firing the next action |
| `ForEachModelAction` | Iterates over models matching a predicate, applies action to each |
| `SequentialActionList` | Executes multiple actions in sequence |

### Action Config Types

| Type | Config Class | Description |
|------|-------------|-------------|
| `set-attribute` | `SetAttributeActionConfig` | Set attribute value with optional duration (creates `SetAndRestore`). Supports `conditionQuery` for conditional application. |
| `send-notification` | `SendNotificationActionConfig` | Send notification message with templated parameters. Parameter types: `ATTRIBUTEVALUE`, `CONSTANT`, `DATETIME`. |

### Action Builders

`Actions.java` provides builder helpers:

- `buildSetValue()` — set an attribute
- `buildSendAction()` — send a message
- `buildActionList()` — combine multiple actions into a sequence

---

## Rule Templates and Catalog

### Rule Template

`RuleTemplate` is a catalog entry that users instantiate into rules:

| Property | Description |
|----------|-------------|
| `id` | Unique template identifier |
| `name`, `description` | Display text |
| `template` | Description text (supports variable substitution) |
| `keywords`, `tags` | Search/categorization |
| `categories` | Template categories (e.g., "Lights", "Security") |
| `premium` | Requires premium service level |
| `populations` | Which populations can use this template |
| `condition` | `ConditionConfig` defining the trigger |
| `actions` | List of `ActionTemplate` defining what happens |
| `options` | Map of `SelectorGenerator` for user choices |
| `satisfiableIf` | Predicate to check if template is applicable |

### Rule Catalog

`RuleCatalog` is an in-memory index of available templates:

- `getById(id)` — look up template by ID
- `getTemplates()` — all templates
- `getCategories()` — category list
- `getTemplatesForCategory(cat)` — filtered list
- `getRuleCountByCategory()` — summary stats
- `merge(other)` — combine catalogs

### Catalog Loading

Templates are defined in XML and deserialized by `RuleCatalogDeserializer`:

- SAX-based parsing with processor chain:
  - `RuleCatalogProcessor` → `RuleTemplatesProcessor` → `RuleTemplateProcessor`
  - `ConditionsProcessor`, `ActionsProcessor` — trigger/action definitions
  - `SelectorsProcessor`, `ParametersProcessor` — user options
- `RuleCatalogLoader` loads catalog by place population
- Catalogs are cached and merged per population

### Selector Generators

When a user creates a rule, selectors provide the available choices:

| Generator | Description |
|-----------|-------------|
| `MinMaxSelectorGenerator` | Numeric range selector |
| `TemperatureSelectorGenerator` | Temperature value selector |
| `PresenceSelectorGenerator` | Person/presence list selector |
| `ConstantListSelectorGenerator` | Enum/constant value selector |

Each generator has `isSatisfiable()` (are options available for this place?) and `generate()` (produce a `Selector` with choices).

---

## Variable Resolution and Templating

### Templated Expressions

Rule templates use `${variable}` syntax for parameter substitution:

```
"Turn on ${device} when ${sensor} detects motion"
```

Variables are resolved when:
1. User creates rule from template, providing context values
2. `TemplatedExpression` / `TemplatedValue` substitute at instantiation
3. Variables stored in rule definition for re-evaluation

### Function Factory

`FunctionFactory` creates functions for templated expressions:

| Function | Purpose |
|----------|---------|
| `CoerceFunction` | Type conversion |
| `GetAttributeValue` | Read model attribute |
| `ToAddressFunction` | String → Address |
| `ParseQueryFunction` | Parse query string |
| `CreateStringPredicateFunction` | String matching predicate |
| `CreateAddressPredicateFunction` | Address matching predicate |
| `CreateMapPredicateFunction` | Map matching predicate |
| `GetAccountOwnerQuery` | Account owner lookup |
| `GetCurrentTimeFormatted` | Timezone-aware time formatting |

---

## Rule Context

### RuleContext

`RuleContext` extends both `ConditionContext` and `ActionContext`:

```java
interface RuleContext extends ConditionContext, ActionContext {
    RuleContext override(String namespace);
    boolean isDirty();
    Map<String, Object> getDirtyVariables();
    void clearDirty();
}
```

### PlatformRuleContext

The full platform implementation provides:

| Capability | Method |
|------------|--------|
| Model access | `getModelByAddress()`, model store queries |
| Variables | `getVariable()`, `setVariable()` (marks dirty for DB sync) |
| Messaging | `send()`, `broadcast()`, `request()` via `PlatformMessageBus` |
| Scheduling | `wakeUpIn(duration)`, `wakeUpAt(time)` via event loop |
| Time | `getLocalTime()` (timezone-aware from place config) |
| Logging | Per-rule logger |

Components:
- Address source (rule address)
- Place ID
- `PlatformMessageBus` for sending messages
- `RuleModelStore` for model access
- `PlaceExecutorEventLoop` for scheduling
- TimeZone from place configuration
- Mutable variables map (dirty-tracked for persistence)

---

## Rule Lifecycle

### Creation

1. User selects `RuleTemplate` and provides context variables
2. `RuleTemplateCapability.CreateRule()` called
3. `RuleTemplate.create()` instantiates `StatefulRuleDefinition`
4. Variables templated into description
5. `ConditionConfig` and `ActionConfig` generated from template
6. `RuleDao.save()` persists to Cassandra (sequenceId=-1, Cassandra assigns next ID)

### Activation

1. `RuleHandler` created for rule
2. `Rule.isSatisfiable()` checked (both condition and action must be satisfiable)
3. If satisfiable: `Rule.activate()` called
   - `Condition.activate()` schedules initial events if needed
   - `Action.activate()` prepares resources
4. Rule becomes active in executor event loop

### Execution

1. Event arrives (e.g., `AttributeValueChangedEvent`)
2. `PlaceExecutorEventLoop` routes to `RuleHandler`
3. `RuleHandler.onEvent()`:
   - Check service level (premium rules on premium accounts)
   - Update satisfiability if changed
   - Call `Rule.execute(event)`
4. `SimpleRule.execute()`:
   - If already firing: `action.keepFiring()` (stateful continuation)
   - Else: check `condition.shouldFire()`
   - If fires: `action.execute()`, track `ActionState`
5. `RuleHandler.syncContextVariables()` saves dirty variables to database

### Suspension / Disabling

1. User calls `Rule.Disable()` or system disables (e.g., downgrade from premium)
2. `RuleHandler.disable()` called
3. `Rule.deactivate()` clears state, cancels scheduled events
4. `RuleDefinition.disabled` flag set
5. `RuleDao.save()` persists state change

### Deletion

1. User calls `Rule.Delete()` or rule becomes permanently unsatisfiable
2. `RuleHandler` stops handling events
3. `Rule.deactivate()` cleanup
4. `RuleDao.delete()` removes from database

### Variable Updates

1. User calls `Rule.UpdateContext()` with new context variables
2. `RuleHandler` syncs with DB via `updateVariables()`
3. Triggers recalculation of selectors/conditions as needed

---

## Scene Framework

Scenes are simpler than rules — no conditions, just actions that execute on demand.

### Scene Components

| Class | Purpose |
|-------|---------|
| `SceneHandler` | Handles scene model changes and execution |
| `PlatformSceneContext` | Provides model store, messaging, action context |
| `SceneActionBuilder` | Builds scene actions from templates, validates premium |

### Scene Lifecycle

- **Creation** — user selects action templates, provides variables
- **Firing** — `Scene.fire()` executes actions in sequence
- **Modification** — rebuilds when actions/notification/name change
- **Deletion** — `Scene.delete()` removes from system

Scenes can also be triggered as actions within rules.

---

## Execution Threading Model

### PlaceExecutorEventLoop

Each place gets a **single-threaded executor** for rule/scene processing:

- Maintains queue of pending events
- Executes all rules/scenes for the place sequentially (no concurrency within a place)
- Handles MDC (Mapped Diagnostic Context) for logging

### DefaultPlaceExecutor

Executes all rules and scenes for a single place:

| Component | Purpose |
|-----------|---------|
| `SingleThreadDispatcher` | Queues events for sequential execution |
| `RuleModelStore` | Thread-safe cache of device models for the place |
| `Map<PlaceEventHandler>` | `RuleHandler` and `SceneHandler` instances |

Methods:
- `setHandlers()` — late-bind handlers (avoids circular dependency)
- `onEvent()` — route event to matching handlers
- `start()` / `stop()` / `reload()` — handler lifecycle

### PlaceExecutorRegistry

Factory and registry for `PlaceExecutor` instances:

- `getExecutor(placeId)` — get or create executor (loads rule environment on first access)
- `start()` / `reload()` / `stop()` — per-place lifecycle
- `clear()` — shutdown all executors

`DefaultPlaceExecutorRegistry` maintains executor pool with async event processing.

### RuleModelStore

Thread-safe cache of device models for a place:

- Updates on events (attribute changes, model add/remove)
- Notifies listeners of changes
- Used by rules to query current model state without DB access

---

## Persistence

### RuleDao

| Operation | Description |
|-----------|-------------|
| `listByPlace(placeId)` | Get all rules for a place |
| `findById(placeId, actionId)` | Retrieve specific rule |
| `save(RuleDefinition)` | Create or update rule |
| `updateVariables(id, variables, modified)` | Update only context variables |
| `delete(placeId, actionId)` | Delete rule |
| `create(Place, RuleDefinition)` | Optimized creation flow |

### Cassandra Schema

`RuleDaoImpl` stores rules in the `RuleEnvironmentTable`:

| Column | Description |
|--------|-------------|
| `CREATED`, `MODIFIED` | Timestamps |
| `NAME`, `DESCRIPTION`, `TAGS` | Metadata |
| `DISABLED`, `SUSPENDED` | State flags |
| `TEMPLATE2` | Template ID this rule was created from |
| `VARIABLES` | JSON-serialized context variables map |
| `ACTIONCONFIG` | Serialized action configuration |
| `CONDITIONCONFIG` | Serialized condition configuration |

### RuleEnvironment

Container for all rules, scenes, and actions for a place:

```java
class RuleEnvironment {
    UUID placeId;
    Map<Integer, RuleDefinition> rules;
    Map<Integer, SceneDefinition> scenes;
    Map<Integer, ActionDefinition> actions;
}
```

`RuleEnvironmentDao` loads/streams entire environments:

- `streamAll()` — stream all rule environments (for migration/batch operations)
- `findByPlace(placeId)` — load full environment for a place
- `deleteByPlace(placeId)` — clean up on place deletion

---

## Rule Events

### RuleEvent Types

| Type | Event Class | Description |
|------|-------------|-------------|
| `ATTRIBUTE_VALUE_CHANGED` | `AttributeValueChangedEvent` | Device attribute changed (address, name, old/new value) |
| `MODEL_ADDED` | `ModelAddedEvent` | New device/model added to place |
| `MODEL_REMOVED` | `ModelRemovedEvent` | Device removed from place |
| `MESSAGE_RECEIVED` | `MessageReceivedEvent` | Platform message received |
| `SCHEDULED_EVENT` | `ScheduledEvent` | Timer/timeout fired |
| `ACTION_COMPLETED` | — | Stateful action completed |
| `EXECUTOR_RESTART` | — | Executor restarted (re-evaluate rules) |

### Event Routing

1. `RuleModelStore` notifies listeners of model changes
2. Events dispatched through `PlaceExecutorEventLoop`
3. Handlers filter by `RuleEventType` (optimization — `handlesEventsOfType()`)
4. Only handlers interested in the event type receive it

---

## Key Files

### Core Rule Engine (`platform/arcus-rules/`)

| File | Description |
|------|-------------|
| `.../common/rule/Rule.java` | Rule interface |
| `.../common/rule/simple/SimpleRule.java` | Concrete rule combining condition + action |
| `.../common/rule/RuleContext.java` | Rule context interface |
| `.../common/rule/condition/Condition.java` | Condition interface |
| `.../common/rule/trigger/SimpleTrigger.java` | Stateless trigger base |
| `.../common/rule/trigger/ValueChangeTrigger.java` | Attribute change trigger |
| `.../common/rule/trigger/QueryChangeTrigger.java` | Query state-change trigger |
| `.../common/rule/trigger/DurationTrigger.java` | Duration-based trigger |
| `.../common/rule/filter/Filters.java` | Time-of-day and day-of-week filters |
| `.../common/rule/action/Action.java` | Action interface |
| `.../common/rule/action/SendAction.java` | Send message action |
| `.../common/rule/action/stateful/SetAndRestore.java` | Set attribute, restore after duration |
| `.../common/rule/action/stateful/ForEachModelAction.java` | Iterate models action |
| `.../common/rule/action/stateful/SequentialActionList.java` | Sequential action chain |
| `.../common/rule/event/RuleEvent.java` | Base event class |
| `.../common/rule/matcher/ModelPredicateMatcher.java` | Model filter for triggers |

### Rule Service (`platform/arcus-containers/rule-service/`)

| File | Description |
|------|-------------|
| `.../rule/service/RuleService.java` | Service entry point (ListTemplates, ListRules, etc.) |
| `.../rule/environment/PlatformRuleContext.java` | Full rule context implementation |
| `.../rule/environment/RuleHandler.java` | Per-rule event handler (lifecycle, sync) |
| `.../rule/environment/SceneHandler.java` | Per-scene event handler |
| `.../rule/environment/DefaultPlaceExecutor.java` | Single-threaded place executor |
| `.../rule/environment/PlaceExecutorRegistry.java` | Executor factory/registry |
| `.../rule/environment/RuleModelStore.java` | Thread-safe model cache |

### Rule Library (`platform/arcus-lib/`)

| File | Description |
|------|-------------|
| `.../platform/rule/RuleDefinition.java` | Abstract rule definition |
| `.../platform/rule/StatefulRuleDefinition.java` | Concrete definition with configs |
| `.../platform/rule/RuleDao.java` | Rule persistence interface |
| `.../platform/rule/cassandra/RuleDaoImpl.java` | Cassandra implementation |
| `.../platform/rule/RuleEnvironment.java` | Container for place's rules/scenes/actions |
| `.../platform/rule/catalog/RuleTemplate.java` | Template catalog entry |
| `.../platform/rule/catalog/RuleCatalog.java` | In-memory template index |
| `.../platform/rule/catalog/serializer/RuleCatalogDeserializer.java` | XML catalog parser |
| `.../platform/rule/catalog/action/config/ActionConfig.java` | Action config interface |
| `.../platform/rule/catalog/action/config/SetAttributeActionConfig.java` | Set-attribute action config |
| `.../platform/rule/catalog/action/config/SendNotificationActionConfig.java` | Notification action config |
| `.../platform/rule/catalog/condition/config/ConditionConfig.java` | Condition config interface |
| `.../platform/rule/catalog/condition/config/ValueChangeConfig.java` | Value-change condition config |
| `.../platform/rule/catalog/function/FunctionFactory.java` | Templated expression functions |
