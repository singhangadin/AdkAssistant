package com.example.adkassistant.agent

import com.example.adkassistant.BuildConfig
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini

/**
 * The assistant agent: a cloud Gemini model plus the local [DeviceTools].
 *
 * `@JvmField` is required — the KSP processor discovers the agent by reading the
 * field directly, and the generated `generatedTools()` extension is what wires
 * the annotated tool functions into the agent.
 */
object AssistantAgent {

    @JvmField
    val rootAgent = LlmAgent(
        name = "device_assistant",
        description = "A helpful assistant that can read the device clock and hardware info.",
        model = Gemini(
            name = "gemini-flash-latest",
            apiKey = BuildConfig.GEMINI_API_KEY,
        ),
        instruction = Instruction(
            "You are a concise on-device assistant. " +
                "When the user asks about the time, use the 'getCurrentTime' tool. " +
                "When they ask about the phone or device, use the 'getDeviceInfo' tool. " +
                "Never invent values you can get from a tool."
        ),
        tools = DeviceTools().generatedTools(),
    )
}
