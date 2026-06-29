# AWS GenAI: Bedrock, RAG, and Agents Gold Sheet

> Track: AWS Interview Track — GenAI Platform
> Goal: build production-ready RAG pipelines and AI agents on AWS Bedrock and explain safety, cost, and evaluation at MAANG depth.

---

## 0. How To Read This

Beginner focus:
- What Bedrock is and what models are available
- Basic InvokeModel API
- RAG concept (retrieval-augmented generation)

Intermediate focus:
- Bedrock Knowledge Bases (managed RAG)
- Bedrock Agents (tool use / function calling)
- Bedrock Guardrails for safety
- Chunking and embedding strategies

Senior / MAANG focus:
- RAG architecture trade-offs (chunk size, overlap, embedding model)
- Agent action groups and Lambda orchestration
- Guardrails content filtering, PII masking, grounding checks
- Cost estimation and token optimization
- Multi-model evaluation and selection
- Production RAG observability
- Bedrock model customization (fine-tuning, continued pre-training)

---

# Topic 1: Amazon Bedrock

## 1. What Bedrock Is

Bedrock is a serverless API that gives access to foundation models from multiple providers:

| Provider | Models |
|---|---|
| Anthropic | Claude 3 Opus, Sonnet, Haiku; Claude 3.5 Sonnet, Haiku |
| Amazon | Titan Text, Titan Embeddings, Titan Image |
| Meta | Llama 3 (8B, 70B) |
| Mistral AI | Mistral 7B, Mixtral 8x7B |
| Cohere | Command R, Command R+ |
| Stability AI | Stable Diffusion (image generation) |
| AI21 Labs | Jurassic-2 |

No servers to manage. Pay per token (input + output tokens).

## 2. Invoking Models

Converse API (recommended — unified across models):

```python
import boto3

bedrock = boto3.client('bedrock-runtime', region_name='us-east-1')

response = bedrock.converse(
    modelId='anthropic.claude-3-5-sonnet-20241022-v2:0',
    messages=[
        {
            'role': 'user',
            'content': [{'text': 'Explain circuit breakers in distributed systems.'}]
        }
    ],
    system=[
        {'text': 'You are an expert AWS solutions architect. Be concise and technical.'}
    ],
    inferenceConfig={
        'maxTokens': 1000,
        'temperature': 0.1,
        'topP': 0.9
    }
)

print(response['output']['message']['content'][0]['text'])
print(f"Input tokens: {response['usage']['inputTokens']}")
print(f"Output tokens: {response['usage']['outputTokens']}")
```

InvokeModel (model-specific format):

```python
import json

body = json.dumps({
    "anthropic_version": "bedrock-2023-05-31",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 512
})

response = bedrock.invoke_model(
    modelId='anthropic.claude-3-haiku-20240307-v1:0',
    body=body
)
result = json.loads(response['body'].read())
```

Converse API preferred: consistent interface across models, supports streaming and tool use.

## 3. Model Selection Framework

| Model | Best For | Cost Tier |
|---|---|---|
| Claude 3.5 Sonnet | complex reasoning, long context, code | high |
| Claude 3 Sonnet | balanced quality/cost | medium |
| Claude 3 Haiku | fast responses, simple tasks | low |
| Llama 3 70B | open-weight, good reasoning | medium |
| Llama 3 8B | lightweight, cost-sensitive | low |
| Titan Embeddings v2 | text embeddings for RAG | low |
| Cohere Command R+ | RAG-optimized, citation support | medium |

Interview answer:

```text
Model selection depends on task complexity, latency requirements, and cost tolerance.
For complex reasoning in a CTO-facing report: Claude 3.5 Sonnet.
For high-volume customer support classification: Llama 3 8B or Claude 3 Haiku.
For embeddings: Titan Embeddings v2 or Cohere Embed for English.
Always evaluate multiple models on your specific use case — benchmark before committing.
```

---

# Topic 2: Bedrock Knowledge Bases (Managed RAG)

## 1. RAG Architecture

RAG supplements LLM responses with context retrieved from your documents:

```text
Without RAG:
  User: "What is our refund policy?"
  LLM: "I don't have information about your specific refund policy."

With RAG:
  User: "What is our refund policy?"
  -> Embed query
  -> Vector search in knowledge base -> returns 3 policy document chunks
  -> Inject chunks into LLM prompt
  -> LLM: "According to your policy document: refunds within 30 days..."
```

## 2. Knowledge Base Components

```text
Data Sources:
  - S3 buckets (PDFs, TXTs, HTML, Markdown, Word, CSV)
  - Web crawl (crawl specific URLs)
  - Confluence, Salesforce, SharePoint (managed connectors)

Chunking Strategy:
  - Fixed size (e.g., 300 tokens per chunk with 10% overlap)
  - Semantic chunking (split at sentence boundaries)
  - Hierarchical chunking (parent chunk + child chunks)

Embedding Model:
  - Titan Embeddings v2 (1,536 dimensions, AWS-native)
  - Cohere Embed Multilingual (multilingual support)

Vector Store:
  - OpenSearch Serverless (AWS-managed, default)
  - Aurora PostgreSQL with pgvector
  - Pinecone, Weaviate (third-party)
  - Redis Enterprise

Sync:
  - On-demand sync or scheduled sync from S3
  - Incremental sync for efficiency
```

## 3. Chunking Strategy Trade-Offs

| Strategy | Chunk Size | Good For | Risk |
|---|---|---|---|
| Fixed-size small (256 tokens) | precise, fast | exact fact retrieval | cuts sentences mid-thought |
| Fixed-size large (1024 tokens) | more context | complex topics | noisy context, more tokens |
| Semantic | varies | natural language coherence | more expensive to compute |
| Hierarchical | parent 1024 + child 256 | precision + context | most complex |

Production starting point:
- 512 tokens per chunk
- 10-20% overlap
- Semantic chunking if budget allows
- Evaluate retrieval quality with test queries before production

## 4. Querying Knowledge Bases

```python
bedrock_agent_runtime = boto3.client('bedrock-agent-runtime')

response = bedrock_agent_runtime.retrieve_and_generate(
    input={'text': "What is the refund policy?"},
    retrieveAndGenerateConfiguration={
        'type': 'KNOWLEDGE_BASE',
        'knowledgeBaseConfiguration': {
            'knowledgeBaseId': 'KBXXXXXXXX',
            'modelArn': 'arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-sonnet-20240229-v1:0',
            'retrievalConfiguration': {
                'vectorSearchConfiguration': {
                    'numberOfResults': 5
                }
            }
        }
    }
)

answer = response['output']['text']
citations = response['citations']  # source chunks used
```

Retrieve only (without generation):

```python
response = bedrock_agent_runtime.retrieve(
    knowledgeBaseId='KBXXXXXXXX',
    retrievalQuery={'text': "refund policy"},
    retrievalConfiguration={
        'vectorSearchConfiguration': {'numberOfResults': 5}
    }
)
chunks = response['retrievalResults']
```

---

# Topic 3: Bedrock Agents

## 1. What Agents Do

Agents enable LLMs to take actions, not just answer questions:

```text
User: "Check my order status and if it's delayed, submit a refund request."

Agent orchestration:
  1. LLM decides: need to check order status
  2. Calls action group: get_order_status(orderId="12345")
  3. Lambda: queries DynamoDB -> returns {status: "delayed", daysLate: 3}
  4. LLM decides: order is delayed, user wants refund
  5. Calls action group: submit_refund(orderId="12345", reason="delivery delay")
  6. Lambda: creates refund record -> returns {refundId: "R-999", status: "processing"}
  7. LLM: "Your order #12345 is delayed. I've submitted refund request R-999."
```

## 2. Agent Components

```text
Agent:
  Foundation model (Claude, etc.)
  Instructions (system prompt)
  Action Groups
  Knowledge Bases (optional)

Action Group:
  OpenAPI schema: defines available functions, parameters, responses
  Lambda function: implements the actions
  OR: AWS service integration (DynamoDB, S3, Bedrock, Lambda, etc.)

Session:
  Maintains conversation context across turns
  Session attributes: pass data between turns
  Memory (optional): persist context across sessions
```

## 3. Action Group Lambda

```python
def lambda_handler(event, context):
    api_path = event['apiPath']
    agent = event['agent']
    action_group = event['actionGroup']
    
    if api_path == '/order/status':
        order_id = event['parameters'][0]['value']
        order = get_order(order_id)
        return {
            'messageVersion': '1.0',
            'response': {
                'actionGroup': action_group,
                'apiPath': api_path,
                'httpMethod': event['httpMethod'],
                'httpStatusCode': 200,
                'responseBody': {
                    'application/json': {
                        'body': json.dumps({
                            'orderId': order['id'],
                            'status': order['status'],
                            'estimatedDelivery': order['eta']
                        })
                    }
                }
            }
        }
```

## 4. Knowledge Base + Agent Integration

Agents can retrieve from Knowledge Bases automatically:

```text
User: "What is the refund policy for international orders?"
Agent:
  1. Determines this requires knowledge base lookup
  2. Retrieves refund policy chunks from knowledge base
  3. Incorporates into response with citations
  4. If user then says: "Apply this to my order" -> uses action groups
```

---

# Topic 4: Bedrock Guardrails

## 1. What Guardrails Do

Guardrails apply safety filters to both prompts and model responses:

| Filter | What It Does |
|---|---|
| Content filtering | block harmful content: hate speech, sexual content, violence |
| PII masking | detect and mask personal data in responses |
| Topic denial | block specific topics (e.g., competitor discussion) |
| Word filters | block specific words or phrases |
| Grounding check | verify response is grounded in retrieved context (not hallucinated) |
| Contextual grounding | detect if response contradicts the retrieved context |

## 2. PII Masking Configuration

```python
guardrail_config = {
    'guardrailIdentifier': 'arn:aws:bedrock:...:guardrail/abc123',
    'guardrailVersion': '1'
}

response = bedrock.converse(
    modelId='anthropic.claude-3-sonnet-...',
    messages=[...],
    guardrailConfig=guardrail_config
)

# PII in model output automatically masked:
# "Customer John Smith (john@example.com) was charged..."
# -> "Customer [NAME] ([EMAIL]) was charged..."
```

## 3. Grounding Check

Prevents hallucinations in RAG responses:

```text
Source context: "Refunds are processed within 30 days."
Model response: "Refunds are processed within 7 days." <- hallucination

Grounding check: compares response against retrieved context
Threshold: 0.75 (higher = stricter)
If below threshold: guardrail blocks or flags response
```

---

# Topic 5: Cost, Observability, And Model Evaluation

## 1. Bedrock Token Costs (Approximate, 2025)

| Model | Input per 1K tokens | Output per 1K tokens |
|---|---|---|
| Claude 3.5 Sonnet | $0.003 | $0.015 |
| Claude 3 Sonnet | $0.003 | $0.015 |
| Claude 3 Haiku | $0.00025 | $0.00125 |
| Llama 3 70B | $0.00099 | $0.00099 |
| Titan Embeddings v2 | $0.00002 per 1K tokens | n/a |

Cost optimization:

```text
1. Use smaller model for classification/routing tasks
2. Cache common prompts (Prompt Caching for Claude saves up to 90% on cached input tokens)
3. Reduce system prompt length (input tokens cost money)
4. Retrieve fewer chunks (fewer tokens in RAG context)
5. Use batch inference for non-interactive workloads
```

## 2. Bedrock Observability

CloudWatch metrics from Bedrock:
- `InvocationsCount`
- `InputTokenCount`, `OutputTokenCount`
- `InvocationLatency` (P50, P90, P99)
- `InvocationThrottles`

CloudWatch logs:
- enable model invocation logging (prompts + responses)
- for debugging, audit, and cost attribution

Cost attribution:

```text
Tag Bedrock invocations by application:
  bedrock.invoke_model(
    ...,
    tags={'Application': 'CustomerSupport', 'Team': 'platform'}
  )
  Use Cost Explorer by tag to see per-application AI cost
```

## 3. Model Evaluation

Bedrock Model Evaluation:

```text
Create evaluation job:
  - Task: text generation, classification, QA
  - Test dataset: prompt + expected outputs
  - Metrics: ROUGE, BERTScore, accuracy, faithfulness (for RAG)
  
Use to:
  - Compare Claude vs Llama on your specific use case
  - Evaluate fine-tuned model vs base model
  - Measure RAG improvement over base model
```

Custom evaluation with LLM-as-judge:

```python
def evaluate_with_llm_judge(question, answer, reference_answer):
    judge_prompt = f"""
    Question: {question}
    Reference Answer: {reference_answer}
    Model Answer: {answer}
    
    Rate the model answer on accuracy (1-5) and completeness (1-5).
    Respond with JSON: {{"accuracy": N, "completeness": N, "reasoning": "..."}}
    """
    
    response = bedrock.converse(
        modelId='anthropic.claude-3-sonnet-...',
        messages=[{'role': 'user', 'content': [{'text': judge_prompt}]}]
    )
    return json.loads(response['output']['message']['content'][0]['text'])
```

## 4. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Single large chunk size for all documents | evaluate chunk sizes per document type; 512 tokens + 10% overlap is a safe start |
| No grounding check on RAG responses | enable guardrail grounding to detect hallucinations |
| Same model for all tasks | route tasks by complexity: Haiku for classification, Sonnet for synthesis |
| No token usage monitoring | CloudWatch token metrics + budget alert on AI spend |
| Fixed system prompt with thousands of tokens | use Bedrock Prompt Management, optimize prompt length |
| No evaluation before production | run model evaluation on test set before production launch |
| Expose agent actions without validation | validate all action inputs in Lambda; never trust LLM output directly |

## 5. Revision Notes

- Bedrock: serverless API, pay per token, multiple model providers
- Converse API: unified interface across all models for prompting and tool use
- RAG: embed query → vector search → inject chunks → LLM generates with context
- Knowledge Bases: managed RAG with S3 + OpenSearch Serverless; sync on demand
- Agents: multi-step tool use; action groups (OpenAPI + Lambda); knowledge base integration
- Guardrails: content filtering, PII masking, topic denial, grounding check
- Cost optimization: smaller model for simple tasks, prompt caching, fewer RAG chunks
- Always evaluate: model comparison, RAG quality, grounding before production

## 6. Official Source Notes

- Bedrock: <https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html>
- Bedrock Knowledge Bases: <https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html>
- Bedrock Agents: <https://docs.aws.amazon.com/bedrock/latest/userguide/agents.html>
- Bedrock Guardrails: <https://docs.aws.amazon.com/bedrock/latest/userguide/guardrails.html>
- Converse API: <https://docs.aws.amazon.com/bedrock/latest/userguide/conversation-inference.html>
