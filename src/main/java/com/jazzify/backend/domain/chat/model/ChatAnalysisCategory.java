package com.jazzify.backend.domain.chat.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum ChatAnalysisCategory {

	OVERVIEW(
		"overview",
		"Overview",
		"🎼",
		"Provide a concise overview of this chord progression:\n"
			+ "- Overall harmonic structure and form (A/B sections, turnarounds)\n"
			+ "- Key center(s) and any modulations\n"
			+ "- Most notable harmonic features (in 2-3 bullet points)\n"
			+ "- General character and style of the harmony\n"
			+ "Keep it short — this is a summary, not deep analysis."
	),
	FUNCTIONAL(
		"functional",
		"Functional Harmony",
		"🔗",
		"Analyze the functional harmony of this progression:\n"
			+ "- Label each chord's function: Tonic (T), Subdominant (SD), Dominant (D)\n"
			+ "- Show the functional flow bar-by-bar (e.g. T → D → D → SD → ...)\n"
			+ "- Highlight any unusual functional assignments\n"
			+ "- Explain the overall tonal trajectory and cadence points\n"
			+ "Present as a clear bar-by-bar table or flow, then explain key moments."
	),
	II_V_I(
		"iiVI",
		"ii-V-I Patterns",
		"🔄",
		"Identify and explain all ii-V-I patterns and their variants:\n"
			+ "- List every ii-V-I (complete and incomplete) with bar numbers\n"
			+ "- Note variants: minor ii-V-i, tritone subs, backdoor ii-Vs\n"
			+ "- Explain how each ii-V resolves (or doesn't)\n"
			+ "- Show the chain/connection between consecutive ii-V-I patterns\n"
			+ "Format each pattern clearly with bar numbers and chord symbols."
	),
	SECONDARY(
		"secondary",
		"Secondary Dominants",
		"⚡",
		"Analyze all secondary dominants and dominant chains:\n"
			+ "- List every secondary dominant with its target (V/vi, V/ii, V/V, etc.)\n"
			+ "- Track dominant chains (sequences of V→V→V resolving down)\n"
			+ "- Note which secondary dominants resolve and which are deceptive\n"
			+ "- Explain the voice leading that makes each secondary dominant work\n"
			+ "Show the chain of dominants as a clear progression diagram."
	),
	MODAL(
		"modal",
		"Modal Interchange",
		"🎨",
		"Analyze modal interchange and borrowed chords:\n"
			+ "- Identify every non-diatonic chord that comes from a parallel mode\n"
			+ "- Specify the source mode (minor, dorian, phrygian, lydian, etc.)\n"
			+ "- Explain the emotional/color effect of each borrowed chord\n"
			+ "- Note any chromatic voice leading created by modal interchange\n"
			+ "If there are no clear modal interchange chords, explain why the non-diatonic chords are better analyzed differently."
	),
	IMPROV(
		"improv",
		"Improvisation",
		"🎹",
		"Give practical improvisation advice for this progression:\n"
			+ "- Suggest scales/modes for each chord or chord group\n"
			+ "- Highlight guide tones and voice leading paths across changes\n"
			+ "- Point out chromatic approach opportunities\n"
			+ "- Suggest target notes for key resolution points\n"
			+ "- Note any \"tricky\" changes that need special attention\n"
			+ "Be specific with note names and scale choices, not just generic advice."
	);

	private final String id;
	private final String label;
	private final String emoji;
	private final String prompt;

	ChatAnalysisCategory(String id, String label, String emoji, String prompt) {
		this.id = id;
		this.label = label;
		this.emoji = emoji;
		this.prompt = prompt;
	}

	@JsonValue
	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public String emoji() {
		return emoji;
	}

	public String prompt() {
		return prompt;
	}

	@JsonCreator
	public static ChatAnalysisCategory from(String value) {
		for (ChatAnalysisCategory category : values()) {
			if (category.id.equalsIgnoreCase(value)) {
				return category;
			}
		}
		throw new IllegalArgumentException("Unknown chat analysis category: " + value);
	}

	public static @Nullable ChatAnalysisCategory fromNullable(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return from(value);
	}
}

