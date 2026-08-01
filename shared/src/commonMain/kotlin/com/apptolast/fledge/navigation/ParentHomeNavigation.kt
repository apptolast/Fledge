package com.apptolast.fledge.navigation

import com.apptolast.fledge.domain.model.ChildProfileId

fun parentChildHomeRoute(childProfileId: ChildProfileId): ChildHomeRoute = ChildHomeRoute(childProfileId.value)
