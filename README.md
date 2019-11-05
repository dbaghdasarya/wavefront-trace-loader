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
#### - Advanced way
