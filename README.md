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
#### - Loading traces from file
If the following option is set in the `config` file the Loader will read traces from the
 specified file and re-ingest them.
```
wfTracesFile: "<traces_source_file_path>"
```
####- Number of program execution
If cycle is equal Infinite:
```
cycle: Infinite
```
The program will execute infinitely.

If cycle is equal positive number:
```
cycle: 3
```
The program will execute given times.
Any other option will cause exception.
For more details see section `Re-Ingestion`
###### For more details see section `Re-Ingestion`

#### - Saving traces to file
For just generation of traces and saving them to file add the following line to the `applicationConfig.yaml` file:
```
spanOutputFile: "<span_output_file_path>"
traceOutputFile: "<trace_output_file_path>"
```
`spanOutputFile` will contain a plain list of spans compatible for providing as a spans source to
 other loader tools (for instance `loadgen`), `traceOutputFile` will contain consistent traces in
  JSON format convenient for further analysis.
 Be aware that this option has the highest priority, and if it exists traces will be saved to
  file regardless of other options.


#### - Providing Trace Types files
 To provide Trace Types files through `yaml` configuration add `inputJsonFiles:` option to the
`applicationConfig.yaml` file.
###### For further details see section `Execution using yaml configuration file`

#### - Direct Ingestion to Wavefront
For direct ingestion of traces to Wavefront add the following lines to the `applicationConfig.yaml` file:
```
server: "Wavefront_hostname" # "http://localhost:8080"
token: "Wavefront_token" "bdc66030-a1a8-493b-b416-5559fdcfa45d"
```
IMPORTANT: disable or remove `spanOutputFile:` and `traceOutputFile:` options.
Follow the [instruction](https://docs.wavefront.com/users_account_managing.html#generating-an-api-token) for getting your Wavefront token.
### The traces generation
#### Simple way
The traces generation parameters could be simply provided via command line options:
- `--duration=00h00m00s` - Duration of ingestion time.
- `--errorRate=10` - Percentage of erroneous traces (0-100). Default: 0
- `--debugRate=20` - Percentage of debug spans in traces (0-100). Default: 0
- `--rate=50` - Rate at which the spans will be ingested (integer number of spans per second). Default: 100
- `--traceTypesCount=5` - Number of traces types for auto-generation. Default: 3
- `--totalTraceCount=16` - Total number of traces for generation. If this parameter greater
 than 0, `duration` will be ignored. Default: 5 minute
- `--stat output.json` - To dump statistics about generated traces to the given file in JSON format.
 By default the statistics will be shown in the console.
#### Advanced way
##### Pattern 
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
      "spanNameSuffixes": "abcdef",
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
          "spanNames": ["name_a", "name_b"],
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
- `"debugRate"` - is applicable only if `"traceTypesCount" > 0`.
- `"traceTypePatterns"` - is a list of traces type patterns. This option is disabled if `"traceTypesCount" > 0`.
    - `"traceTypeName"` - this name will be set to the root span of a trace.
    - `"spanNameSuffixes"` - a list of possible suffixes of span names. Span names generated as
     `name_`+`suffix character` (this field is optional).
    - `"nestingLevel"` - number of levels in the trace tree.
    - `"tracePercentage"` - percentage of traces of the given trace type among all generated traces.
    - `"errorRate"` - if `"traceTypePatterns"` exists, every trace type pattern should provide `"errorRate"` value for producing erroneous traces. 
     - `"errorRate"` - if `"traceTypePatterns"` exists, every trace type pattern should provide `"errorRate"` value for producing debug spans in trace type.
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
        - `"spanNames` - list of span names for which the condition is applicable (optional)
        - `"tagName"` - route cause tag of the error condition 
        - `"tagValues"` - route cause value of the tag of the error condition
        - `"errorRate"` - the percentage of the produced errors that meet the given condition. If multiple conditions could be applied the result rate will be P(AB)=P(A)+P(B)-P(A)*P(B), P(ABC) = P(AB)+P(C)-P(AB)*P(C) ...

##### Topology 
This method allows to define a strict topology of the generated traces via topology file (`json`):
- `-f topology.json` - Generator config file.

This option has lower priority than `traceTypePatterns` section and will be ignored in case both
 are present in the same file.
The `json` file has the following structure:
```
{
  "spansRate": 50,
  "duration": "2m",
  "totalTraceCount": 100,
  "traceTopology": {
    "traceTypes": [
      {
        "tracePercentage": 80,
        "spansCount": 15,
        "errorRate": 20,
        "debugRate": 30,
        "traceDurations": [
          {
            "startValue": 50,
            "endValue": 100,
            "percentage": 20
          },
          ...
        ],
        "errorConditions": [
          {
            "spanNames": ["taxi_001"],
            "tagName": "application",
            "tagValue": "TestApp2",
            "errorRate": 80
          },
          {
            "tagName": "source",
            "tagValue": "ip-12.3.4.5",
            "errorRate": 90
          }
        ]
      },
      ...
    ],
    "serviceConnections": [
      {
        "root": true,
        "services": [
          "order",
          "request"
        ],
        "children": [
          "overnight",
          "event",
          "other"
        ]
      },
      {
        "services": [
          "overnight"
        ],
        "children": [
          "front-desk",
          "room-service",
          "taxi"
        ]
      },
      ...
    ],
    "serviceTags": [
      {
        "services": [
          "*"
        ],
        "mandatoryTags": [
          {
            "tagName": "application",
            "tagValues": [
              "TestApp2"
            ]
          },
          {
            "tagName": "source",
            "tagValues": [
              "ip-10.1.2.3",
              "ip-11.2.3.4",
              ...
            ]
          },
          ...
        ],
        "optionalTags": [
          {
            "tagName": "days",
            "tagValues": [
              5,
              ...
            ]
          },
          ...
        ],
        "optionalTagsPercentage": 90
      }
    ],
    "serviceSpansNumbers": [
      {
        "services": [
          "*"
        ],
        "spansNumber": 2
      },
      {
        "services": [
          "order",
          "request"
        ],
        "spansNumber": 1
      }
    ]
  }
}
```

`"spansRate"`, `"duration"` and `"totalTraceCount"` keys have the same meaning that the similar
 command line options.
- `"traceTopology"` - the main block that defines topology of the generated traces.
    - `"traceTypes"` - a list of trace types generated according the defined topology.
        - `"tracePercentage"` - a percentage of traces of the given trace type in
         `"totalTraceCount"`.
        - `"spansCount"` - number of spans in a trace of this type.
        - `"errorRate"` - if this value set, traces will be generated with error tag in the root
         span and all other error conditions will be ignored.
        - `"debugRate"` - if this value set, traces will be generated with debug tag in the root
         span.
        - `"traceDurations"` - distribution of durations among traces of the given trace type.
            - `"startValue"`, `"endValue"` - range of the duration in milliseconds.
            - `"percentage"` - a percentage of the traces with duration in the given range.
        - `"errorConditions"` - conditions on which errors will be generated. `"errorConditions"` disables all previously defined `"errorRates"`.
            - `"spanNames` - list of span names for which the condition is applicable (optional)
            - `"tagName"` - route cause tag of the error condition 
            - `"tagValues"` - route cause value of the tag of the error condition
            - `"errorRate"` - the percentage of the produced errors that meet the given condition. If multiple conditions could be applied the result rate will be P(AB)=P(A)+P(B)-P(A)*P(B), P(ABC) = P(AB)+P(C)-P(AB)*P(C) ...
    - `"serviceConnections"` - defines hierarchy of services.
        - `"root"` - if `true` then services can be used as a service of a root span. Default
         value is `false.`
        - `"services"` - a list of services that can be parent services. The wildcard is allowed.
        - `"children"` - a list of services that can be children of the services from the
         previous list.  
    - `"serviceTags"` - defines tags applicable to services.
        - `"services"` - a list of services. The wildcard is allowed.
        - `"mandatoryTags"` - some of the span tags are mandatory, it means that every span will have the given set of tags. Some tags are defined as mandatory by Wavefront (`application`, `service`). Trace generator will add missing mandatory tags and will warn the user about it.
            - `"tagName"` - name of the tag.
            - `"tagValues"` - list of possible values. The Generator will randomly select values from the list.
        - `"optionalTags"` - some tags are optional and may be missing in the tags list of a span.
            - `"tagName"` - name of the tag.
            - `"tagValues"` - list of possible values. The Generator will randomly select values from the list.
        - `"optionalTagsPercentage"` - defines a percentage of optional tags wich should be added to span. For instance, if a user provides 5 optional tags and sets `"optionalTagsPercentage": 40` every span will have randomly selected 2 optional tags.
    - `"serviceSpansNumbers"` - defines spans numbers of the listed traces. The default value is 1.
        - `"services"` - a list of services. The wildcard is allowed.
        - `"spansNumber"` - number of spans of the given service. For example if number = 2, can
         be generated 2 types of spans for the given service with names `<service_name>_001
         ` and `<service_name>_002`. 


#### - Example
### Execution using `yaml` configuration file

Set Trace Types topology/pattern (`json`) files in `yaml` configuration file like this:
````
inputJsonFiles:
 - "topology.json"
 - "pattern.json"
 ````
Run the program.
No need to provide command line arguments, if provided command line arguments
will be ignored. To execute the program using command line arguments simply disable
or remove `inputJsonFiles:` option.

### Execution from the "wavefront-trace-loader" directory

```
$ java -jar target/wavefront-trace-loader-1.0-SNAPSHOT-jar-with-dependencies.jar -f pattern.json
 --configFile anyConfig.yaml
```

### Re-Ingestion
It is possible to generate some set of traces, save it to file and then send the same set from
 the file to Wavefront. The Loader will automatically update UUIDs and timestamps.
User can specify start of trace loading by adding the following line in any place of the
 source file, and starting from this line traces will be shifted in time according to user's input:
```
start_ms: <timestamp_in_ms> 
```
Also, user can add the following string to modify spans and traces duration according to given
 condition - spanName, tagName, tagValue, delta (shift in percentages) and probability (in
  percentage). The probability defines number of matching cases that should be updated. 
```
latency: {  "spanName":"<Name of a span>", "tagName":"Name of a tag", "tagValue":"Tag value", 
            "delta":<percentage>, "probability":<percentage>}
```
##Statistics
If the user is interested in sending the statistics directly to the Wavefront, he/she can 
uncomment the `reportStat` field in the `yaml` file. This way, a trace bearing the statistical 
data will be sent to the same cluster and to the same customer as the other traces. Additionally,
one can specify the cluster, and the token of the customer to which the statistics should be sent 
via the fields `statServer` and `statToken` correspondingly.
