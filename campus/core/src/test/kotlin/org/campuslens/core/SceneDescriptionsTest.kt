package org.campuslens.core

import org.junit.Assert.*
import org.junit.Test

class SceneDescriptionsTest {
 private fun detection(label: String, score: Float=.9f)=Detection(label,score,Box(0f,0f,20f,20f))
 private fun describe(items: List<Detection>): SceneReply {
  val scene=SceneDescriptions();scene.observe(items,7,100)
  return scene.describe(7,200,true,.55f)
 }
 @Test fun groupsRepeatedLabelsAndCountsActualRecords() {
  assertEquals("I can identify two people and a chair.",describe(listOf(detection("person"),detection("chair"),detection("person"))).text)
 }
 @Test fun usesActualLabelsWithoutInventingRelationships() {
  assertEquals("I can identify a bottle and a dining table.",describe(listOf(detection("dining table"),detection("bottle"))).text)
 }
 @Test fun emptyResultsDoNotClaimSafety() {
  assertEquals("I could not identify the surroundings clearly.",describe(emptyList()).text)
 }
 @Test fun lowConfidenceResultsDoNotBecomeSceneFacts() {
  assertEquals(describe(emptyList()).text,describe(listOf(detection("person",.2f))).text)
 }
 @Test fun atMostThreeLabelGroupsAndStableOrdering() {
  val records=listOf("person","chair","bottle","car","cup").map { detection(it) }
  assertEquals("I can identify a bottle, a car and a chair.",describe(records).text)
  assertEquals(describe(records).text,describe(records.reversed()).text)
 }
 @Test fun observationExpiresAtOneSecondWithoutRefreshingItsTimestamp() {
  val scene=SceneDescriptions();scene.observe(listOf(detection("chair")),7,100)
  assertEquals(100L,scene.describe(7,1100,true,.55f).observedMs)
  val stale=scene.describe(7,1101,true,.55f)
  assertNull(stale.observedMs);assertFalse(stale.text.contains("chair"))
 }
 @Test fun futureTimestampIsRejected() {
  val scene=SceneDescriptions();scene.observe(listOf(detection("chair")),7,100)
  assertNull(scene.describe(7,99,true,.55f).observedMs)
 }
 @Test fun oldSessionAndClearedObservationsAreRejected() {
  val scene=SceneDescriptions();scene.observe(listOf(detection("chair")),7,100)
  assertNull(scene.describe(8,200,true,.55f).observedMs)
  scene.clear();assertNull(scene.describe(7,200,true,.55f).observedMs)
 }
 @Test fun unavailableCameraOrDetectorDoesNotDescribeCachedObjects() {
  val scene=SceneDescriptions();scene.observe(listOf(detection("chair")),7,100)
  val reply=scene.describe(7,200,false,.55f)
  assertTrue(reply.text.contains("unavailable"));assertFalse(reply.text.contains("chair"));assertNull(reply.observedMs)
 }
 @Test fun higherThresholdAppliesToPreviouslyCachedDetections() {
  val scene=SceneDescriptions();scene.observe(listOf(detection("chair",.7f)),7,100)
  assertEquals(describe(emptyList()).text,scene.describe(7,200,true,.8f).text)
 }
 @Test fun snapshotDoesNotAccumulateCountsAcrossFramesOrRetainMutableList() {
  val scene=SceneDescriptions();val items=mutableListOf(detection("chair"))
  scene.observe(items,7,100);items.add(detection("person"))
  assertEquals("I can identify a chair.",scene.describe(7,200,true,.55f).text)
  scene.observe(listOf(detection("person")),7,210)
  assertEquals("I can identify a person.",scene.describe(7,220,true,.55f).text)
 }
 @Test fun automaticIdenticalAnnouncementsAreSuppressed() {
  val scene=describe(listOf(detection("chair")));val policy=SpeechPolicy()
  assertNotNull(policy.offer(scene.speech(200,false),200));policy.finished(300)
  assertNull(policy.offer(scene.speech(400,false),400))
 }
 @Test fun explicitRequestCanRepeatFreshSceneAfterSpeechFinishes() {
  val scene=describe(listOf(detection("chair")));val policy=SpeechPolicy()
  policy.offer(scene.speech(200,false),200);policy.finished(300)
  assertNotNull(policy.offer(scene.speech(400,true),400))
  assertNull(policy.offer(scene.speech(500,true),500)) // never queue the same active utterance
  assertNull(policy.finished(600))
 }
 @Test fun requestedDescriptionCannotOverrideSystemSpeechAndExpiresInQueue() {
  val policy=SpeechPolicy();val system=SpeechEvent("Camera unavailable",Priority.SYSTEM,0)
  policy.offer(system,0)
  assertNull(policy.offer(describe(listOf(detection("chair"))).speech(200,true),200))
  assertEquals(system,policy.active);assertNull(policy.finished(1101))
 }
 @Test fun ordinaryDescriptionCannotOverrideOcr() {
  val policy=SpeechPolicy();val ocr=SpeechEvent("ROOM 204",Priority.RESULT,0)
  policy.offer(ocr,0)
  assertNull(policy.offer(describe(listOf(detection("chair"))).speech(200,false),200))
  assertEquals(ocr,policy.active)
 }
 @Test fun commandParserAcceptsDescriptionAndPreservesExistingCommands() {
  assertEquals(Command.DESCRIBE,CommandParser.parse("Describe my surroundings."))
  assertEquals(Command.DESCRIBE,CommandParser.parse("What can you see?"))
  assertEquals(Command.READ,CommandParser.parse("Read text"))
  assertEquals(Command.STOP,CommandParser.parse("Stop assistance"))
  assertEquals(Command.LOCATE,CommandParser.parse("Where am I?"))
  assertEquals(Command.LOCATE,CommandParser.parse("Find my location"))
  assertNull(CommandParser.parse("Is it safe to cross?"))
 }
}
