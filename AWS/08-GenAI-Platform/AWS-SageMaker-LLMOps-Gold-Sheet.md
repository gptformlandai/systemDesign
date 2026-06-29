# AWS GenAI: SageMaker and LLMOps Gold Sheet

> Track: AWS Interview Track — GenAI Platform
> Goal: understand SageMaker deployment patterns, implement LLMOps practices, and decide when to use Bedrock vs SageMaker.

---

## 0. How To Read This

Beginner focus:
- SageMaker purpose and key concepts
- SageMaker real-time endpoints
- Bedrock vs SageMaker decision

Intermediate focus:
- SageMaker endpoint types (real-time, serverless, async, batch)
- SageMaker model registry
- SageMaker Pipelines for training workflows
- Model monitoring

Senior / MAANG focus:
- LLMOps: model versioning, evaluation, progressive rollout
- SageMaker JumpStart vs Bedrock
- SageMaker multi-model and multi-container endpoints
- Token cost control in production LLM systems
- Prompt versioning and A/B testing
- Model drift and output quality monitoring
- Fine-tuning workflow on SageMaker

---

# Topic 1: Amazon SageMaker

## 1. What SageMaker Does

SageMaker is a fully managed ML platform for building, training, and deploying models.

Use SageMaker when:
- you need to train custom ML models (not foundation models from Bedrock)
- you need fine-tune open-source LLMs (Llama, Mistral) with your own data
- you need maximum control over the model, infrastructure, and inference
- you deploy models from SageMaker JumpStart (curated pre-trained models)

Use Bedrock when:
- you want serverless access to Claude, Titan, Llama, Mistral without managing infrastructure
- you want managed RAG (Knowledge Bases) and Agents
- operational simplicity is the priority

## 2. SageMaker Endpoint Types

| Endpoint Type | Latency | Traffic | Best For |
|---|---|---|---|
| Real-time | synchronous, <1s | up to tens of thousands RPS | interactive apps, APIs |
| Serverless | synchronous, cold start | sporadic/intermittent | dev, testing, low-traffic |
| Asynchronous | async, minutes | large payloads, long inference | batch-like, document processing |
| Batch Transform | batch, hours | large datasets | offline scoring, bulk inference |

## 3. Real-Time Endpoint

```python
import boto3
import json

# Create endpoint configuration
sagemaker = boto3.client('sagemaker')

sagemaker.create_endpoint_config(
    EndpointConfigName='payment-fraud-model-config',
    ProductionVariants=[{
        'VariantName': 'AllTraffic',
        'ModelName': 'payment-fraud-model-v2',
        'InitialInstanceCount': 2,
        'InstanceType': 'ml.g4dn.xlarge',
        'InitialVariantWeight': 1.0
    }]
)

sagemaker.create_endpoint(
    EndpointName='payment-fraud-endpoint',
    EndpointConfigName='payment-fraud-model-config'
)

# Invoke
runtime = boto3.client('sagemaker-runtime')

response = runtime.invoke_endpoint(
    EndpointName='payment-fraud-endpoint',
    ContentType='application/json',
    Body=json.dumps({'transaction_amount': 1500.00, 'merchant_category': 'electronics'})
)
result = json.loads(response['Body'].read())
print(f"Fraud score: {result['fraud_score']}")
```

## 4. Serverless Endpoint

No instance management, scales to zero:

```python
sagemaker.create_endpoint_config(
    EndpointConfigName='serverless-config',
    ProductionVariants=[{
        'VariantName': 'AllTraffic',
        'ModelName': 'my-model',
        'ServerlessConfig': {
            'MemorySizeInMB': 4096,
            'MaxConcurrency': 20
        }
    }]
)
```

Cold start: 5-15 seconds for LLMs. Use for dev/test or non-latency-sensitive apps.

## 5. Async Endpoint

For large payloads or long-running inference:

```python
# Upload input to S3
s3.put_object(Bucket='inputs', Key='request-123.json', Body=json.dumps(payload))

# Submit async inference request
response = runtime.invoke_endpoint_async(
    EndpointName='my-async-endpoint',
    InputLocation='s3://inputs/request-123.json',
    ContentType='application/json'
)

output_location = response['OutputLocation']
# Poll or use EventBridge notification when output is ready in S3
```

Use for:
- document summarization (minutes of inference)
- large image/video processing
- batch-like workloads that don't need immediate response

## 6. Auto Scaling Real-Time Endpoints

Scale instances based on invocation rate:

```python
aas_client = boto3.client('application-autoscaling')

# Register scalable target
aas_client.register_scalable_target(
    ServiceNamespace='sagemaker',
    ResourceId='endpoint/payment-fraud-endpoint/variant/AllTraffic',
    ScalableDimension='sagemaker:variant:DesiredInstanceCount',
    MinCapacity=1,
    MaxCapacity=10
)

# Target tracking: scale to maintain 70 invocations per instance per minute
aas_client.put_scaling_policy(
    PolicyName='InvocationsScaling',
    ServiceNamespace='sagemaker',
    ResourceId='endpoint/payment-fraud-endpoint/variant/AllTraffic',
    ScalableDimension='sagemaker:variant:DesiredInstanceCount',
    PolicyType='TargetTrackingScaling',
    TargetTrackingScalingPolicyConfiguration={
        'TargetValue': 70.0,
        'PredefinedMetricSpecification': {
            'PredefinedMetricType': 'SageMakerVariantInvocationsPerInstance'
        }
    }
)
```

---

# Topic 2: SageMaker Model Registry And Pipelines

## 1. Model Registry

Version and track models:

```python
sagemaker.create_model_package(
    ModelPackageGroupName='fraud-detection-models',
    ModelPackageDescription='Fraud model v3 — trained on Jan 2025 data',
    InferenceSpecification={
        'Containers': [{
            'Image': '...',
            'ModelDataUrl': 's3://my-models/fraud-v3/model.tar.gz'
        }],
        'SupportedContentTypes': ['application/json'],
        'SupportedResponseMIMETypes': ['application/json']
    },
    ModelApprovalStatus='PendingManualApproval'  # or Approved after evaluation
)
```

Stages:
1. Model trained → registered as `PendingManualApproval`
2. Evaluation metrics attached (accuracy, AUC, etc.)
3. Human review → approve or reject
4. Approved → deploy to staging, then prod

## 2. SageMaker Pipelines

Orchestrate ML training workflows:

```python
from sagemaker.workflow.pipeline import Pipeline
from sagemaker.workflow.steps import ProcessingStep, TrainingStep, CreateModelStep
from sagemaker.workflow.model_step import ModelStep

pipeline = Pipeline(
    name='FraudDetectionPipeline',
    steps=[
        ProcessingStep(
            name='PreprocessData',
            processor=SKLearnProcessor(...)
        ),
        TrainingStep(
            name='TrainModel',
            estimator=XGBoost(...)
        ),
        CreateModelStep(
            name='CreateModel',
            model=model
        )
    ]
)

pipeline.upsert(role_arn=role)
pipeline.start()
```

## 3. Model Monitoring

SageMaker Model Monitor detects:
- Data drift: input distribution shifts from training baseline
- Model quality: accuracy/AUC degrades over time
- Bias drift: prediction bias changes

Setup:

```python
from sagemaker.model_monitor import DataCaptureConfig, DefaultModelMonitor

# Enable data capture on endpoint
data_capture_config = DataCaptureConfig(
    enable_capture=True,
    sampling_percentage=20,
    destination_s3_uri='s3://my-monitoring/captures/'
)

# Create monitoring schedule
monitor = DefaultModelMonitor(
    role=role,
    instance_count=1,
    instance_type='ml.m5.xlarge'
)

monitor.create_monitoring_schedule(
    monitor_schedule_name='fraud-monitor',
    endpoint_input=EndpointInput('payment-fraud-endpoint', '/opt/ml/processing/input'),
    statistics=baseline_statistics,
    constraints=baseline_constraints,
    schedule_cron_expression='cron(0 * ? * * *)'  # hourly
)
```

---

# Topic 3: LLMOps

## 1. Prompt Versioning

Prompts are code. Treat them like code:

```text
Prompt registry (DynamoDB or SSM Parameter Store):
  - Store prompt templates with version IDs
  - Tag with: created_at, author, model, task, evaluation_score
  - Never hardcode prompts in application code

Deployment:
  - Prompts deployed via CI/CD independent of code
  - Roll back prompt independently of code release
  - A/B test: 90% old prompt, 10% new prompt
```

AWS AppConfig for prompts:

```text
AppConfig application: LLMPrompts
Environment: prod
Configuration: JSON with prompt templates
Deployment strategy: canary (10% → 25% → 100%)

Application fetches prompt from AppConfig at runtime.
Update prompt without redeploying app.
Rollback = revert AppConfig deployment.
```

## 2. LLM Response Evaluation In Production

Online evaluation (sample of live traffic):

```python
def evaluate_response_sample(question, response, reference_answer=None):
    # LLM-as-judge evaluation on sampled responses
    judge_prompt = f"""
    Evaluate this customer support response on a scale of 1-5:
    
    Customer Question: {question}
    Agent Response: {response}
    
    Criteria:
    - Accuracy: Is the information correct?
    - Helpfulness: Does it solve the problem?
    - Tone: Is it professional and empathetic?
    
    Output JSON: {{"accuracy": N, "helpfulness": N, "tone": N, "reasoning": "..."}}
    """
    
    # Use separate judge model (not the same model being evaluated)
    result = call_bedrock(judge_prompt)
    
    # Log to CloudWatch custom metric
    cloudwatch.put_metric_data(
        Namespace='LLMOps/CustomerSupport',
        MetricData=[
            {'MetricName': 'AccuracyScore', 'Value': result['accuracy'], 'Unit': 'None'},
            {'MetricName': 'HelpfulnessScore', 'Value': result['helpfulness'], 'Unit': 'None'}
        ]
    )
    
    return result
```

Alarm: accuracy score drops below 3.5 → alert for prompt or model drift.

## 3. Token Cost Control

Track and optimize token usage:

```python
class TrackedBedrockClient:
    def __init__(self):
        self.bedrock = boto3.client('bedrock-runtime')
        self.cloudwatch = boto3.client('cloudwatch')
    
    def converse(self, model_id, messages, system=None, **kwargs):
        response = self.bedrock.converse(
            modelId=model_id,
            messages=messages,
            system=system or [],
            **kwargs
        )
        
        usage = response['usage']
        
        # Track per-feature token costs
        self.cloudwatch.put_metric_data(
            Namespace='LLMOps/TokenCosts',
            MetricData=[
                {
                    'MetricName': 'InputTokens',
                    'Dimensions': [
                        {'Name': 'Feature', 'Value': kwargs.get('feature', 'unknown')},
                        {'Name': 'Model', 'Value': model_id}
                    ],
                    'Value': usage['inputTokens'],
                    'Unit': 'Count'
                },
                {
                    'MetricName': 'OutputTokens',
                    'Dimensions': [
                        {'Name': 'Feature', 'Value': kwargs.get('feature', 'unknown')},
                        {'Name': 'Model', 'Value': model_id}
                    ],
                    'Value': usage['outputTokens'],
                    'Unit': 'Count'
                }
            ]
        )
        
        return response
```

Cost reduction strategies:

```text
1. Prompt caching (Claude): cache system prompt prefix (up to 90% discount on cached tokens)
2. Smaller model for simpler subtasks
3. Response caching: DynamoDB/ElastiCache for repeated identical queries
4. Streaming: stream response, show partial output, stop early if user found answer
5. RAG chunk optimization: retrieve fewer chunks; smaller chunks
6. Budget alerts: CloudWatch alarm on monthly token cost estimate
```

## 4. Model Rollout For LLMs

Progressive model upgrade:

```text
Current: Claude 3 Sonnet (model_id: anthropic.claude-3-sonnet-...)
New: Claude 3.5 Sonnet (model_id: anthropic.claude-3-5-sonnet-...)

Rollout strategy:
  1. Offline evaluation: run both models on 500-question evaluation set
     Compare: accuracy, helpfulness, cost per token, latency
  2. Shadow mode: route 5% to Claude 3.5, log responses for comparison
     Human review of shadow responses for 1 week
  3. A/B test: 10% Claude 3.5, 90% Claude 3 Sonnet
     Monitor: evaluation scores, latency, token cost per session
  4. Gradual increase: 25% → 50% → 100% if metrics positive
  5. Full rollout or rollback based on metrics
```

## 5. Fine-Tuning On SageMaker

When to fine-tune:
- base model doesn't follow domain-specific format or terminology
- you have high-quality labeled examples (>1,000 examples)
- inference cost savings matter (smaller fine-tuned model can match larger base)
- consistent structured output required

Fine-tuning workflow:

```text
1. Prepare dataset: JSONL format, instruction → response pairs
2. SageMaker JumpStart fine-tuning job (or custom training job)
3. Model registry: register fine-tuned model with evaluation metrics
4. Deploy to SageMaker endpoint (or export and deploy on Bedrock custom model)
5. A/B test fine-tuned vs base model
```

Fine-tuning dataset format (JSONL):

```json
{"prompt": "<human>Classify this transaction: Coffee $4.50\n<assistant>", "completion": "category: food_beverage, subcategory: coffee_shop, confidence: 0.99"}
{"prompt": "<human>Classify this transaction: AWS charges $450.00\n<assistant>", "completion": "category: technology, subcategory: cloud_services, confidence: 0.98"}
```

## 6. Bedrock vs SageMaker Decision

| Factor | Choose Bedrock | Choose SageMaker |
|---|---|---|
| Model type | managed foundation models | custom ML, fine-tuned LLMs, self-hosted |
| Infrastructure | no management | full control over instances |
| RAG | managed Knowledge Bases | custom pipeline (if Bedrock limits insufficient) |
| Cost model | per token | per instance-hour |
| Compliance | Bedrock model privacy terms apply | your data, your GPU |
| Speed to production | fastest | requires MLOps setup |

Bedrock for most teams. SageMaker when you need fine-tuned private models, specific hardware, or Bedrock's model selection is insufficient.

## 7. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Hardcode model ID in application | use AppConfig or Parameter Store; model upgrade = config change |
| No token cost tracking | instrument all LLM calls with token metrics |
| Deploy fine-tuned model without evaluation | always compare fine-tuned vs base on test set before promotion |
| Same model for all tasks | route by complexity: small model for classification, large for generation |
| No prompt versioning | treat prompts as code, version in registry |
| Skip shadow testing before model upgrade | shadow mode first, then gradual rollout |
| No output quality monitoring | LLM-as-judge on sampled traffic; alarm on quality drop |

## 8. Revision Notes

- SageMaker: custom models, fine-tuning, full infra control; Bedrock: managed foundation models
- Real-time endpoint: synchronous, low latency; serverless: cold start; async: long inference
- SageMaker Model Registry: version, approve, deploy; tracks metadata + evaluation
- SageMaker Pipelines: orchestrate preprocessing + training + registration
- Prompt versioning: AppConfig or SSM Parameter Store; deploy prompts independent of code
- Token cost: track per feature, set budget alarms; optimize with caching and smaller models
- LLM rollout: offline eval → shadow → A/B → gradual → full
- Fine-tune: only when base model genuinely insufficient; needs >1K quality examples

## 9. Official Source Notes

- SageMaker: <https://docs.aws.amazon.com/sagemaker/latest/dg/whatis.html>
- SageMaker endpoints: <https://docs.aws.amazon.com/sagemaker/latest/dg/deploy-model.html>
- SageMaker Pipelines: <https://docs.aws.amazon.com/sagemaker/latest/dg/pipelines.html>
- SageMaker Model Monitor: <https://docs.aws.amazon.com/sagemaker/latest/dg/model-monitor.html>
- SageMaker JumpStart: <https://docs.aws.amazon.com/sagemaker/latest/dg/studio-jumpstart.html>
- Bedrock model customization: <https://docs.aws.amazon.com/bedrock/latest/userguide/custom-models.html>
