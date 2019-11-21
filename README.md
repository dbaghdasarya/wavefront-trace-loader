# Wavefront Trace Loader

Wavefront Trace Loader (WTL) is a tool for the generation of traces and loading them to a destination. The destination could be [Wavefront](https://www.wavefront.com/) or text file.
Ingestion to the Wavefront could be implemented by Direct Ingestion mechanism, or through the [Wavefront Proxy](https://docs.wavefront.com/proxies.html) (the most recommended way).

## Getting started!

WTL is developed using Java 11 (Language Level - 11). The further instructions are applicable to Mac OS. To run the tool it should be built, and properly configured, For ingestion to Wavefront a Wavefront account is required with enabled tracing. Additionally, Wavefront Proxy could be used.

### Getting from Git
```sh
$ mkdir <your_preferrable_dir>
$ cd <your_preferrable_dir>
$ git clone git@github.com:dbaghdasarya/wavefront-trace-loader.git
$ cd wavefront-trace-loader/
$ mvn clean install
```

### Configuration
Application configuration should be set via a `yaml` file. The default version is provided with the sources - `applicationConfig.yaml`.
#### - Saving traces to file
For just generation of traces and saving them to file add the following line to the `applicationConfig.yaml` file:
```
outputFile: "<output_file_path>"
```
Be aware that this option has the highest priority, and if it exists traces will be saved to file regardless of other options.

#### - Direct Ingestion to Wavefront
For direct ingestion of traces to Wavefront add the following lines to the `applicationConfig.yaml` file:
```
server: "Wavefront_hostname" # "http://localhost:8080"
token: "Wavefront_token" "bdc66030-a1a8-493b-b416-5559fdcfa45d"
```
IMPORTANT: disable or remove `outputFile:` option.
Follow the [instruction](https://docs.wavefront.com/users_account_managing.html#generating-an-api-token) for getting your Wavefront token.
### The traces generation
#### - Simple way
The traces generation parameters could be simply provided via command line options:
- `--duration=00h00m00s` - Duration of ingestion time.
- `--errorRate=10` - Percentage of erroneous traces (0-100). Default: 0
- `--rate=50` - Rate at which the spans will be ingested (integer number of spans per second). Default: 100
- `--traceTypesCount=5` - Number of traces types for auto-generation. Default: 0
- `--totalTraceCount=16` - Total number of traces for generation. If this parameter greater
 than 0, `duration` will be ignored. Default: 0
- `--stat output.json` - To dump statistics about generated traces to the given file in JSON format.
 By default the statistics will be shown in the console.
#### - Advanced way
The traces generation parameters could be set via Trace Types Pattern file (`json`):
- `-f pattern.json` - Generator config file.

This option will disable all other generation related options.
The `json` file has the following structure:
```
{
  "spansRate": 50,
  "duration": "2m",
  "errorRate": 20,
  "totalTraceCount": 16,
  "traceTypesCount": 5,
  "traceTypePatterns": [
    {
      "traceTypeName": "TType_1",
      "traceTypesCount": 20,
      "errorRate": 15,
      "nestingLevel": 5,
      "tracePercentage": 50,
      "spansDistributions": [
        {
          "startValue": 10,
          "endValue": 15,
          "percentage": 10
        },
        {
          "startValue": 20,
          "endValue": 40,
          "percentage": 90
        }
      ],
      "traceDurations": [
        {
          "startValue": 50,
          "endValue": 100,
          "percentage": 20
        },
        {
          "startValue": 200,
          "endValue": 500,
          "percentage": 80
        }
      ],
      "mandatoryTags": [
        {
          "tagName": "application",
          "tagValues": [...]
        },
        {
          "tagName": "service",
          "tagValues": [...]
        }
      ],
      "optionalTags": [
        {
          "tagName": "some_param1",
          "tagValues": [...]
        },
        {
          "tagName": "payment2",
          "tagValues": [...]
        },
        {
        "tagName": "loyalty",
        "tagValues": [
            "dinners",
            "platinum",
            "gold",
            "standard"
        ]
        }
      ],
      "optionalTagsPercentage": -1,
      "errorConditions": [
        {
          "tagName": "loyalty",
          "tagValue": "platinum",
          "errorRate": 80
        },
        ...
      ]
    }
  ]
}
```

`"spansRate"`, `"duration"`, `"traceTypesCount"`, `"errorRate"` and `"totalTraceCount"` keys have the same meaning that
 the similar command line options.
- `"errorRate"` - is applicable only if `"traceTypesCount" > 0`.
- `"traceTypePatterns"` - is a list of traces type patterns. This option is disabled if `"traceTypesCount" > 0`.
    - `"traceTypeName"` - This name will be set to the root span of a trace.
    - `"nestingLevel"` - number of levels in the trace tree.
    - `"tracePercentage"` - percentage of traces of the given trace type among all generated traces.
    - `"errorRate"` - if `"traceTypePatterns"` exists, every trace type pattern should provide `"errorRate"` value for producing erroneous traces. 
    - `"spansDistributions"` - distribution of spans count among traces of the given trace type.
        -  `"startValue"` , `"endValue"` - range of the spans count.
        -  `"percentage"` - a percentage of the traces with spans count in the given range.
    - `"traceDurations"` - distribution of durations among traces of the given trace type.
        - `"startValue"`, `"endValue"` - range of the duration in milliseconds.
        - `"percentage"` - a percentage of the traces with duration in the given range.
    - `"mandatoryTags"` - some of the span tags are mandatory, it means that every span will have the given set of tags. Some tags are defined as mandatory by Wavefront (`application`, `service`). Trace generator will add missing mandatory tags and will warn the user about it.
        - `"tagName"` - name of the tag.
        - `"tagValues"` - list of possible values. The Generator will randomly select values from the list.
    - `"optionalTags"` - some tags are optional and may be missing in the tags list of a span.
        - `"tagName"` - name of the tag.
        - `"tagValues"` - list of possible values. The Generator will randomly select values from the list.
    - `"optionalTagsPercentage"` - defines a percentage of optional tags wich should be added to span. For instance, if a user provides 5 optional tags and sets `"optionalTagsPercentage": 40` every span will have randomly selected 2 optional tags.
    - `"errorConditions"` - conditions on which errors will be generated. `"errorConditions"` disables all previously defined `"errorRates"`.
        - `"tagName"` - route cause tag of the error condition 
        - `"tagValues"` - route cause value of the tag of the error condition
        - `"errorRate"` - the percentage of the produced errors that meet the given condition. If multiple conditions could be applied the result rate will be P(AB)=P(A)+P(B)-P(A)*P(B), P(ABC) = P(AB)+P(C)-P(AB)*P(C) ...
