# Overview

Here an overview of the test will be described ...

# Test flow

![Test flow](img/end-to-end-test-flow.png)

Here an description of the test flow steps ...

# Configure

It is necessary to configure the scripts that will be used by the SAPS pipeline to be able to produce the same result expected by the test, for that, we must configure three components to recognize these test scripts.

### Configuring Dashboard

In the [SAPS script file](/public/dashboardApp.js) replace the value of the variable `scriptsTags` by:

```javascript
let scriptsTags = 
{
"inputdownloading":[
    {
      "name": "inputdownloading-test",
      "docker_tag": "test",
      "docker_repository": "fogbow/inputdownloading"
    }
  ],
  "preprocessing":[
    {
      "name": "preprocessing-test",
      "docker_tag": "test",
      "docker_repository": "fogbow/preprocessor"
    }
  ],
  "processing":[
    {
      "name": "processing-test",
      "docker_tag": "test",
      "docker_repository": "fogbow/worker"
    }
  ]
}
```

### Configuring Dispatcher and Scheduler

In the [SAPS script file](/resources/execution_scripts_tags.json) replace it with the following json:

```json
{
"inputdownloading":[
    {
      "name": "inputdownloading-test",
      "docker_tag": "test",
      "docker_repository": "fogbow/inputdownloading"
    }
  ],
  "preprocessing":[
    {
      "name": "preprocessing-test",
      "docker_tag": "test",
      "docker_repository": "fogbow/preprocessor"
    }
  ],
  "processing":[
    {
      "name": "processing-test",
      "docker_tag": "test",
      "docker_repository": "fogbow/worker"
    }
  ]
}
```

# Run

Here how to perform the test ...

# Check results

Here how to check the test results with the expected ones will be described ...