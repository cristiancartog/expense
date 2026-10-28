# Project instructions

This is the canonical instruction file for AI coding agents in this repository.
Keep project-wide guidance here; `CLAUDE.md` and `GEMINI.md` import this file.

## Project overview

- Android expense-tracking application.
- Java 21 source and target compatibility.
- Gradle project with the application in the `app` module.
- Application package: `ro.pandemonium.expense`.
- Dependency injection: Dagger Hilt.
- Unit tests: JUnit Jupiter and Mockito.

## Common commands

Run commands from the repository root.

- Build: `./gradlew build` (`gradlew.bat build` on Windows).
- Unit tests: `./gradlew test` (`gradlew.bat test` on Windows).
- Android lint: `./gradlew lint` (`gradlew.bat lint` on Windows).
- Clean: `./gradlew clean` (`gradlew.bat clean` on Windows).

## Working agreements

- Preserve the existing project structure and Java conventions unless a task requires a change.
- Keep changes focused on the requested task.
- Add or update tests when behavior changes.
- Run the most relevant available tests after making code changes.
- Do not commit secrets, credentials, local SDK paths, or generated build output.

## Coding guidance

1. in method call chaining, try to align them by the dot on separate lines. example:

```
# instead of 
queryForExpenses(query).stream().findFirst()

# do this
queryForExpenses(query).stream()
				       .findFirst()

```

2. do not automatically break an instruction on new lines just because the text doesn't fit the "maximum" line length,

```
# instead of
var someVaryVeryVeryVeryLongVariableName = SomeUtilityWithAVeryVeryLongHame
		.callingSomeUtilityMethodWithAnotherLongName(someOtherVariableWithAVeryLongName
			.someOtherMethodWithAVeryLongNameReturningSomething())

# leave it on the same line
var someVaryVeryVeryVeryLongVariableName = SomeUtilityWithAVeryVeryLongHame.callingSomeUtilityMethodWithAnotherLongName(someOtherVariableWithAVeryLongName.someOtherMethodWithAVeryLongNameReturningSomething())
```

3.  calls to methods with multiple parameters: if the called method signature accepts parameters of more complex types (not simple primitives and their wrappers), then put all parameters on separate lines, like so (note the ending paranthesis)

```
# instead of this
submitWithProgress(progressPublisher -> result.set(backgroundOperation.get()),  NO_OP_CONSUMER,
        () -> resultConsumer.accept(result.get()), errorConsumer);

# do this
submitWithProgress(  
        progressPublisher -> result.set(backgroundOperation.get()),  
        NO_OP_CONSUMER,  
        () -> resultConsumer.accept(result.get()),  
        errorConsumer  
);
```
