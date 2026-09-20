# Incident note

**Incident:** amount parser selected the wrong rupee figure when a bank message used a whole-rupee amount such as `Rs.5`.

**Root cause:** the original amount regex required exactly two decimal places, so it could skip the transaction amount and match a later balance amount.

**Impact:** 24 logical transactions across 38 source messages in corpus-a were affected by this parsing weakness.

**Fix:** accept zero, one, or two decimal places and normalize every parsed amount to scale 2; added a regression for the water-can message.

**Follow-up:** keep amount extraction tests for whole-rupee, one-decimal and two-decimal bank formats.
