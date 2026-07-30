package com.apptolast.fledge.presentation.foundation.childhome

import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.service.SavingsGoalProjectionStatus

enum class ChildHomeActionKind {
    TaskAddPhoto,
    TaskSubmit,
    GoalDeposit,
    GoalWithdraw,
    CashOut,
    ParentZone,
    Settings,
    Purchase,
    ExternalLink,
    EmptyTasksAction,
    EmptyGoalAction,
}

enum class ChildHomeActionPriority {
    Primary,
    Secondary,
}

enum class ChildHomeIconKey(val symbol: String) {
    Camera("📷"),
    Send("✓"),
    Target("◎"),
    CoinsIn("+"),
    WalletOut("↓"),
    Banknote("💵"),
    Shield("✓"),
    Settings("⚙"),
    ShoppingBag("🛍"),
    ExternalLink("↗"),
    Tasks("☑"),
    Ledger("☰"),
    Clock("⏳"),
    Check("✓"),
    Alert("!"),
    Trophy("🏆"),
    Bike("🚲"),
    Game("🎮"),
    Book("📚"),
}

enum class ChildHomeTextKey {
    TaskAddPhotoLabel,
    TaskAddPhotoDescription,
    TaskSubmitLabel,
    TaskSubmitDescription,
    GoalDepositLabel,
    GoalDepositDescription,
    GoalWithdrawLabel,
    GoalWithdrawDescription,
    GoalWithdrawDisabledDescription,
    CashOutLabel,
    CashOutDescription,
    CashOutDisabledDescription,
    ParentZoneLabel,
    ParentZoneDescription,
    SettingsLabel,
    SettingsDescription,
    PurchaseLabel,
    PurchaseDescription,
    ExternalLinkLabel,
    ExternalLinkDescription,
    EmptyTasksActionLabel,
    EmptyTasksActionDescription,
    EmptyGoalActionLabel,
    EmptyGoalActionDescription,
    TaskStatusPendingLabel,
    TaskStatusSubmittedLabel,
    TaskStatusApprovedLabel,
    TaskStatusRejectedLabel,
    TaskStatusExpiredLabel,
    GoalStatusCompletedLabel,
    GoalStatusOnTrackLabel,
    GoalStatusNeedsContributionLabel,
}

enum class ChildHomeSemanticsRole {
    Button,
}

data class ChildHomeActionSpec(
    val kind: ChildHomeActionKind,
    val priority: ChildHomeActionPriority,
    val iconKey: ChildHomeIconKey,
    val labelKey: ChildHomeTextKey,
    val contentDescriptionKey: ChildHomeTextKey,
    val semanticsRole: ChildHomeSemanticsRole = ChildHomeSemanticsRole.Button,
    val minTouchTargetDp: Int = when (priority) {
        ChildHomeActionPriority.Primary -> 56
        ChildHomeActionPriority.Secondary -> 48
    },
    val isIconFirst: Boolean = true,
    val enforcesExactHeight: Boolean = false,
)

data class ChildHomeStatusVisual(val iconKey: ChildHomeIconKey, val textKey: ChildHomeTextKey)

fun childHomeActionSpec(kind: ChildHomeActionKind): ChildHomeActionSpec = when (kind) {
    ChildHomeActionKind.TaskAddPhoto -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Secondary,
        iconKey = ChildHomeIconKey.Camera,
        labelKey = ChildHomeTextKey.TaskAddPhotoLabel,
        contentDescriptionKey = ChildHomeTextKey.TaskAddPhotoDescription,
    )

    ChildHomeActionKind.TaskSubmit -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Primary,
        iconKey = ChildHomeIconKey.Send,
        labelKey = ChildHomeTextKey.TaskSubmitLabel,
        contentDescriptionKey = ChildHomeTextKey.TaskSubmitDescription,
    )

    ChildHomeActionKind.GoalDeposit -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Primary,
        iconKey = ChildHomeIconKey.CoinsIn,
        labelKey = ChildHomeTextKey.GoalDepositLabel,
        contentDescriptionKey = ChildHomeTextKey.GoalDepositDescription,
    )

    ChildHomeActionKind.GoalWithdraw -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Primary,
        iconKey = ChildHomeIconKey.WalletOut,
        labelKey = ChildHomeTextKey.GoalWithdrawLabel,
        contentDescriptionKey = ChildHomeTextKey.GoalWithdrawDescription,
    )

    ChildHomeActionKind.CashOut -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Primary,
        iconKey = ChildHomeIconKey.Banknote,
        labelKey = ChildHomeTextKey.CashOutLabel,
        contentDescriptionKey = ChildHomeTextKey.CashOutDescription,
    )

    ChildHomeActionKind.ParentZone -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Primary,
        iconKey = ChildHomeIconKey.Shield,
        labelKey = ChildHomeTextKey.ParentZoneLabel,
        contentDescriptionKey = ChildHomeTextKey.ParentZoneDescription,
    )

    ChildHomeActionKind.Settings -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Secondary,
        iconKey = ChildHomeIconKey.Settings,
        labelKey = ChildHomeTextKey.SettingsLabel,
        contentDescriptionKey = ChildHomeTextKey.SettingsDescription,
    )

    ChildHomeActionKind.Purchase -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Secondary,
        iconKey = ChildHomeIconKey.ShoppingBag,
        labelKey = ChildHomeTextKey.PurchaseLabel,
        contentDescriptionKey = ChildHomeTextKey.PurchaseDescription,
    )

    ChildHomeActionKind.ExternalLink -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Secondary,
        iconKey = ChildHomeIconKey.ExternalLink,
        labelKey = ChildHomeTextKey.ExternalLinkLabel,
        contentDescriptionKey = ChildHomeTextKey.ExternalLinkDescription,
    )

    ChildHomeActionKind.EmptyTasksAction -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Secondary,
        iconKey = ChildHomeIconKey.Tasks,
        labelKey = ChildHomeTextKey.EmptyTasksActionLabel,
        contentDescriptionKey = ChildHomeTextKey.EmptyTasksActionDescription,
    )

    ChildHomeActionKind.EmptyGoalAction -> ChildHomeActionSpec(
        kind = kind,
        priority = ChildHomeActionPriority.Primary,
        iconKey = ChildHomeIconKey.Target,
        labelKey = ChildHomeTextKey.EmptyGoalActionLabel,
        contentDescriptionKey = ChildHomeTextKey.EmptyGoalActionDescription,
    )
}

fun childHomeTaskStatusVisual(status: TaskInstanceStatus): ChildHomeStatusVisual = when (status) {
    TaskInstanceStatus.Pending -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.Clock,
        textKey = ChildHomeTextKey.TaskStatusPendingLabel,
    )

    TaskInstanceStatus.Submitted -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.Clock,
        textKey = ChildHomeTextKey.TaskStatusSubmittedLabel,
    )

    TaskInstanceStatus.Approved -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.Check,
        textKey = ChildHomeTextKey.TaskStatusApprovedLabel,
    )

    TaskInstanceStatus.Rejected -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.Alert,
        textKey = ChildHomeTextKey.TaskStatusRejectedLabel,
    )

    TaskInstanceStatus.Expired -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.Alert,
        textKey = ChildHomeTextKey.TaskStatusExpiredLabel,
    )
}

fun childHomeGoalProgressVisual(status: SavingsGoalProjectionStatus): ChildHomeStatusVisual = when (status) {
    SavingsGoalProjectionStatus.Completed -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.Trophy,
        textKey = ChildHomeTextKey.GoalStatusCompletedLabel,
    )

    SavingsGoalProjectionStatus.OnTrack -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.Check,
        textKey = ChildHomeTextKey.GoalStatusOnTrackLabel,
    )

    SavingsGoalProjectionStatus.NeedsContribution -> ChildHomeStatusVisual(
        iconKey = ChildHomeIconKey.CoinsIn,
        textKey = ChildHomeTextKey.GoalStatusNeedsContributionLabel,
    )
}

fun childHomeGoalIconKey(iconKey: String?): ChildHomeIconKey = when (iconKey) {
    "bike" -> ChildHomeIconKey.Bike
    "game" -> ChildHomeIconKey.Game
    "book" -> ChildHomeIconKey.Book
    else -> ChildHomeIconKey.Target
}
