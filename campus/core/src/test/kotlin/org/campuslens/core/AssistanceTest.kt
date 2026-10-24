package org.campuslens.core
import org.junit.Assert.*
import org.junit.Test

class AssistanceTest {
 private fun detection(score: Float)=Detection("chair",score,Box(0f,0f,10f,10f))
 @Test fun thresholdIncludesBoundaryAndOrders() {
  assertEquals(listOf(.9f,.55f),DetectionPolicy.filter(listOf(detection(.54f),detection(.55f),detection(.9f)),.55f).map{it.confidence})
 }
 @Test(expected=IllegalArgumentException::class) fun rejectNanScore() { detection(Float.NaN) }
 @Test(expected=IllegalArgumentException::class) fun rejectInvalidRotation() { FrameMetadata("phone",0,0,640,480,45) }
 @Test(expected=IllegalArgumentException::class) fun rejectEmptyFrame() { FrameMetadata("phone",0,0,0,480,0) }
 @Test fun sessionRejectsPreviousGenerationAfterRestart() {
  val e=AssistanceEngine();val first=e.start();e.ready(first);assertTrue(e.accepts(first,10,20))
  e.stop();val second=e.start();e.ready(first);assertEquals(SessionState.STARTING,e.state)
  e.ready(second);assertFalse(e.accepts(first,10,20));assertTrue(e.accepts(second,10,20))
 }
 @Test fun rejectStaleAndFutureFrames() {
  val e=AssistanceEngine();val t=e.start();e.ready(t)
  assertFalse(e.accepts(t,10,1011));assertFalse(e.accepts(t,50,40))
 }
 @Test fun errorInvalidatesOutstandingWork() {
  val e=AssistanceEngine();val t=e.start();e.ready(t);e.fail();assertFalse(e.accepts(t,0,1))
 }
 @Test fun blankOcrNeverInventsText() { assertTrue(OcrPolicy.message(listOf(OcrLine("  ",null))).startsWith("I could not read")) }
 @Test fun lowConfidenceOcrIsOmitted() {
  assertEquals("Room 204",OcrPolicy.message(listOf(OcrLine("Room 204",.9f),OcrLine("invented",.1f))))
 }
 @Test fun unknownOcrConfidencePreservesObservedText() { assertEquals("Exit",OcrPolicy.message(listOf(OcrLine("Exit",null)))) }
 @Test fun priorityPreemptsAndClearsStaleDescriptions() {
  val p=SpeechPolicy(); val d=SpeechEvent("chair",Priority.DESCRIPTION,0)
  assertEquals(d,p.offer(d,0));assertNull(p.offer(SpeechEvent("table",Priority.DESCRIPTION,1),1))
  val error=SpeechEvent("camera lost",Priority.SYSTEM,2)
  assertEquals(error,p.offer(error,2));assertNull(p.finished(3))
 }
 @Test fun duplicateSuppressionExpires() {
  val p=SpeechPolicy();val e=SpeechEvent("chair",Priority.DESCRIPTION,0)
  p.offer(e,0);p.finished(1);assertNull(p.offer(e.copy(createdMs=2000),2000))
  assertNotNull(p.offer(e.copy(createdMs=8001),8001))
 }
 @Test fun expiredQueuedDescriptionIsDropped() {
  val p=SpeechPolicy();p.offer(SpeechEvent("reading",Priority.RESULT,0),0)
  p.offer(SpeechEvent("chair",Priority.DESCRIPTION,1,1000),1)
  assertNull(p.finished(5000))
 }
 @Test fun queueKeepsOnlyLatestDescription() {
  val p=SpeechPolicy();p.offer(SpeechEvent("reading",Priority.RESULT,0),0)
  p.offer(SpeechEvent("chair",Priority.DESCRIPTION,1),1);p.offer(SpeechEvent("table",Priority.DESCRIPTION,2),2)
  assertEquals("table",p.finished(3)?.text);assertNull(p.finished(4))
 }
 @Test(expected=IllegalArgumentException::class) fun missingModelRejected() { ModelAssetValidator.requireTflite(byteArrayOf()) }
 @Test(expected=IllegalArgumentException::class) fun htmlInsteadOfModelRejected() { ModelAssetValidator.requireTflite(ByteArray(2048){60}) }
 @Test fun voiceCommandsDoNotExecuteUnknownText() {
  assertEquals(Command.STOP,CommandParser.parse("Stop assistance."));assertNull(CommandParser.parse("Cross the road"))
 }
}
