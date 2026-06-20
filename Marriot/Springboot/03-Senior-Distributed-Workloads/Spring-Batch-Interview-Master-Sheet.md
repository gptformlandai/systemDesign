# Spring Batch Interview Master Sheet

Target: Java Backend / Spring Boot / MAANG-style production interviews.

This sheet covers Spring Batch as a complete interview and production topic:
- batch vs API vs scheduler
- Job, Step, JobRepository, JobLauncher
- chunk processing
- ItemReader, ItemProcessor, ItemWriter
- Tasklet steps
- job parameters and job instances
- restartability
- skip, retry, rollback
- partitioning and parallel steps
- idempotency
- scheduling batch jobs
- production monitoring and failure handling

Goal:

```text
After reading this sheet, you should be able to design, implement, debug, and explain
a production Spring Batch job that safely processes large data with restartability,
transactions, idempotency, retries, and monitoring.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Beginner | Job, Step, Reader, Processor, Writer, chunk processing |
| Intermediate | JobRepository, JobParameters, restartability, transactions, skip/retry |
| Senior | idempotency, partitioning, parallelism, backpressure, failure recovery |
| MAANG-ready | explain trade-offs, metadata model, scaling, observability, exactly-once illusions |

Must-say interview line:

```text
Spring Batch is not just a scheduler. It is a framework for reliable offline processing
with metadata, transactions, chunking, restartability, and controlled failure handling.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| Job vs Step | Very high | Core architecture |
| Chunk processing | Very high | Most important execution model |
| Reader / Processor / Writer | Very high | Main coding model |
| JobRepository | Very high | Restartability and metadata |
| JobParameters | High | Job instance identity |
| Restartability | Very high | Production reliability |
| Skip and retry | Very high | Bad records and transient failures |
| Transaction boundary | Very high | Correctness under partial failure |
| Tasklet | Medium-high | Simple one-step tasks |
| Parallel steps | Medium-high | Runtime optimization |
| Partitioning | High | Large data scaling |
| Idempotency | Very high | Prevent duplicate side effects |
| Scheduling | Medium-high | Triggering batch safely |
| Monitoring | High | Operations and debugging |

---

## 2. What Is Spring Batch?

Spring Batch is a framework for building reliable batch jobs.

Batch jobs usually:
- process many records
- run in the background
- may take seconds, minutes, or hours
- need restart after failure
- need audit history
- need controlled retries and skips

Strong answer:

```text
Spring Batch is used for offline or background processing of large data. It gives us
job metadata, chunk-based transactions, restartability, skip/retry handling, and scalable
patterns like partitioning.
```

---

## 3. Batch vs API vs Scheduler

| Concept | Purpose | Example |
|---|---|---|
| REST API | Handle user request now | create booking |
| Scheduler | Trigger something at a time | run every midnight |
| Batch job | Process data reliably | settle yesterday's payments |

Common trap:

```text
@Scheduled is not Spring Batch.
```

Correct:

```text
@Scheduled can trigger a Spring Batch job, but Spring Batch provides the job execution,
metadata, transactions, and restartability.
```

---

## 4. Spring Batch Big Picture

```text
Scheduler / API / CLI
        |
        v
JobLauncher
        |
        v
Job
        |
        v
Step 1 -> Step 2 -> Step 3
        |
        v
Reader -> Processor -> Writer
        |
        v
JobRepository metadata tables
```

What happens:

1. A trigger starts a job using `JobLauncher`.
2. Spring Batch creates or resumes a `JobExecution`.
3. The job runs one or more steps.
4. Each chunk reads items, processes them, and writes them inside a transaction.
5. Metadata is stored in `JobRepository`.
6. If failure happens, the job can restart from saved progress.

---

## 5. Core Vocabulary

| Term | Meaning |
|---|---|
| Job | Complete batch workflow |
| Step | One stage inside a job |
| JobInstance | Logical run identified by job name + identifying parameters |
| JobExecution | One attempt to run a job instance |
| StepExecution | One attempt to run a step |
| JobParameters | Inputs that identify or configure a job run |
| JobRepository | Stores metadata about job/step executions |
| JobLauncher | Starts jobs |
| ItemReader | Reads one item at a time |
| ItemProcessor | Transforms or validates item |
| ItemWriter | Writes a chunk of items |
| ExecutionContext | Restart state storage |

---

## 6. Job

A `Job` is the full batch workflow.

Example:

```java
@Bean
Job settlementJob(JobRepository jobRepository, Step settlementStep) {
    return new JobBuilder("settlementJob", jobRepository)
            .start(settlementStep)
            .build();
}
```

Strong answer:

```text
A Job represents the complete batch process. It is made of one or more steps and is
identified by its name plus identifying job parameters.
```

---

## 7. Step

A `Step` is one unit of work inside a job.

Common step styles:
- chunk-oriented step
- tasklet step
- partitioned step
- flow step

Example:

```java
@Bean
Step settlementStep(JobRepository jobRepository,
                    PlatformTransactionManager transactionManager,
                    ItemReader<Payment> reader,
                    ItemProcessor<Payment, Settlement> processor,
                    ItemWriter<Settlement> writer) {
    return new StepBuilder("settlementStep", jobRepository)
            .<Payment, Settlement>chunk(100, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

Interview line:

```text
Step is where the actual processing strategy is defined.
```

---

## 8. Chunk Processing

Chunk processing means:

```text
Read N items -> process N items -> write N items -> commit transaction
```

Example with chunk size 100:

```text
read 100 records
process 100 records
write 100 records
commit
repeat
```

Why chunking exists:
- avoids loading all data into memory
- gives transaction boundaries
- improves throughput through batch writes
- enables restart from chunk checkpoint

Strong answer:

```text
Chunk processing is the main Spring Batch model. It processes items in groups, and each
chunk usually has its own transaction. If a chunk fails, that chunk can roll back without
reprocessing the entire file or table.
```

---

## 9. Chunk Size Trade-Off

| Small Chunk | Large Chunk |
|---|---|
| less rollback work | better throughput |
| more commits | bigger rollback on failure |
| lower memory pressure | more memory pressure |
| slower for huge jobs | better batch writes |

Rules of thumb:
- start with 100 to 1000 for database jobs
- tune based on write latency, memory, rollback cost, and lock duration
- do not blindly maximize chunk size

Interview trap:

```text
Bigger chunk is always faster.
```

Better:

```text
Bigger chunks can improve throughput, but increase memory use, lock time, and rollback cost.
```

---

## 10. ItemReader

`ItemReader<T>` reads one item at a time.

Common readers:
- `FlatFileItemReader`
- `JdbcCursorItemReader`
- `JdbcPagingItemReader`
- `JpaPagingItemReader`
- custom API reader

Example:

```java
class PaymentReader implements ItemReader<Payment> {
    @Override
    public Payment read() {
        // return next payment
        // return null when input is complete
        return null;
    }
}
```

Important rule:

```text
Returning null means the reader is finished.
```

---

## 11. ItemProcessor

`ItemProcessor<I, O>` transforms or filters data.

Example:

```java
class SettlementProcessor implements ItemProcessor<Payment, Settlement> {
    @Override
    public Settlement process(Payment payment) {
        if (!payment.isSuccessful()) {
            return null; // filters item out
        }
        return new Settlement(payment.getId(), payment.getAmount());
    }
}
```

Important rule:

```text
Returning null from processor filters the item.
```

Use processor for:
- validation
- transformation
- enrichment
- filtering

Avoid processor for:
- slow remote calls inside huge loops unless controlled
- writing to database
- irreversible side effects

---

## 12. ItemWriter

`ItemWriter<O>` writes a chunk of processed items.

Example:

```java
class SettlementWriter implements ItemWriter<Settlement> {
    @Override
    public void write(Chunk<? extends Settlement> chunk) {
        for (Settlement settlement : chunk) {
            // write settlement
        }
    }
}
```

Production rules:
- use batch inserts when possible
- make writes idempotent
- avoid per-item commits inside writer
- do not call slow APIs without timeout and retry strategy

Strong answer:

```text
Writer receives a chunk, not just one item, so it should use efficient bulk writes and
preserve idempotency in case the chunk is retried.
```

---

## 13. Tasklet Step

Tasklet is for simple one-time work.

Examples:
- delete temp files
- call stored procedure
- archive processed file
- send summary email

Example:

```java
@Bean
Step cleanupStep(JobRepository jobRepository,
                 PlatformTransactionManager transactionManager) {
    return new StepBuilder("cleanupStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // cleanup logic
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}
```

Tasklet vs chunk:

| Tasklet | Chunk |
|---|---|
| single task | item-by-item processing |
| simple logic | large record processing |
| cleanup/setup | files, database rows, messages |

---

## 14. JobRepository

`JobRepository` stores metadata.

It tracks:
- job instances
- job executions
- step executions
- status
- start/end time
- read/write/skip counts
- execution context

Why it matters:

```text
Without JobRepository, Spring Batch cannot reliably restart, audit, or know what already ran.
```

Typical metadata tables:
- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_JOB_EXECUTION_PARAMS`
- `BATCH_STEP_EXECUTION`
- `BATCH_STEP_EXECUTION_CONTEXT`
- `BATCH_JOB_EXECUTION_CONTEXT`

Strong answer:

```text
JobRepository is the persistent metadata store that makes Spring Batch production-grade.
It records execution state so failed jobs can be inspected and restarted.
```

---

## 15. JobParameters

Job parameters are inputs to a job.

Example:

```text
settlementDate=2026-06-17
sourceFile=payments-2026-06-17.csv
run.id=42
```

Important:

```text
Job name + identifying JobParameters = JobInstance
```

If you run same job with same identifying parameters after completion, Spring Batch treats
it as the same completed job instance.

Strong answer:

```text
JobParameters identify and configure a job run. For daily jobs, business parameters like
businessDate are usually identifying, while operational parameters may be non-identifying.
```

---

## 16. JobInstance vs JobExecution

| Concept | Meaning |
|---|---|
| JobInstance | Logical job run |
| JobExecution | One attempt to execute that instance |

Example:

```text
JobInstance: settlementJob for businessDate=2026-06-17
JobExecution #1: failed at 01:10
JobExecution #2: restarted and completed at 01:25
```

Interview line:

```text
A failed job instance can have multiple executions.
```

---

## 17. Restartability

Restartability means a failed job can continue safely.

Spring Batch saves state in:
- JobRepository metadata
- StepExecution
- ExecutionContext
- reader checkpoint state

Failure example:

```text
processed 10,000 rows
failure at row 10,250
last committed chunk ended at 10,200
restart begins near 10,201
```

Strong answer:

```text
Spring Batch restartability comes from metadata and chunk checkpoints. On failure, committed
chunks remain committed and uncommitted chunk work rolls back. Restart continues from the
last safe checkpoint, assuming readers and writers are restart-safe.
```

---

## 18. ExecutionContext

`ExecutionContext` stores restart state.

Use cases:
- last processed ID
- file position
- page number
- custom checkpoint

Example:

```java
ExecutionContext context = stepExecution.getExecutionContext();
context.putLong("lastProcessedId", paymentId);
```

Production caution:

```text
Do not store large objects in ExecutionContext. Store small checkpoint values.
```

---

## 19. Transactions In Spring Batch

In chunk processing, transaction usually wraps:

```text
read chunk -> process chunk -> write chunk -> commit metadata
```

If writer fails:
- current chunk rolls back
- already committed chunks remain
- metadata reflects failure
- job can restart

Common trap:

```text
Spring Batch means the whole job is one big transaction.
```

Correct:

```text
Usually each chunk is one transaction. This is why huge jobs can commit progress gradually.
```

---

## 20. Skip

Skip means ignore bad records and continue.

Example:

```java
return new StepBuilder("importStep", jobRepository)
        .<InputRow, Customer>chunk(100, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skip(InvalidRowException.class)
        .skipLimit(100)
        .build();
```

Use skip for:
- malformed rows
- known validation failures
- data quality issues where business allows partial success

Do not skip:
- database outage
- corrupted input file
- unknown exceptions
- payment double-charge risk

Strong answer:

```text
Skip is for record-level bad data, not infrastructure failure. I use skip limits and store
skipped records for later review.
```

---

## 21. Retry

Retry means reattempt transient failures.

Example:

```java
return new StepBuilder("exportStep", jobRepository)
        .<Settlement, Settlement>chunk(50, transactionManager)
        .reader(reader)
        .writer(writer)
        .faultTolerant()
        .retry(TransientDataAccessException.class)
        .retryLimit(3)
        .build();
```

Use retry for:
- temporary database connection failure
- temporary network issue
- deadlock loser
- rate-limited remote service, if backoff is controlled

Do not retry blindly:
- validation error
- duplicate key caused by bad design
- permanent authentication failure

---

## 22. Retry vs Skip

| Retry | Skip |
|---|---|
| try same item again | ignore item and continue |
| transient issue | bad item |
| limited attempts | limited count |
| may eventually succeed | item remains failed |

Strong answer:

```text
I retry transient failures and skip known bad records only when business accepts partial
processing. Both should have limits, logging, and operational visibility.
```

---

## 23. Idempotency

Idempotency means rerunning the same operation does not create duplicate side effects.

Why it matters:

```text
Batch jobs are restarted and retried. Without idempotency, restart can duplicate payments,
emails, exports, or database rows.
```

Patterns:
- unique business key
- upsert instead of insert
- processed marker table
- output file temp name then atomic rename
- external idempotency key
- write-audit before irreversible side effect

Example:

```sql
create unique index uq_settlement_payment
on settlements(payment_id);
```

Strong answer:

```text
I never rely only on in-memory state for batch idempotency. I enforce idempotency using
database constraints, processed markers, or external idempotency keys.
```

---

## 24. Parallel Steps

Parallel steps run independent steps at same time.

Example:

```text
Job
 |-- importCustomersStep
 |-- importRoomsStep
 |-- importRatesStep
```

Use when:
- steps are independent
- database can handle concurrency
- there is no ordering dependency

Avoid when:
- steps write same rows
- downstream step needs previous output
- shared locks cause contention

---

## 25. Partitioning

Partitioning splits one large step into smaller partitions.

Example:

```text
Partition 1: customer_id 1-100000
Partition 2: customer_id 100001-200000
Partition 3: customer_id 200001-300000
```

Why it exists:
- process huge data faster
- use multiple threads or workers
- isolate failures per partition

Strong answer:

```text
Partitioning is used when a single step has too much data. The master step creates ranges
or partitions, and worker steps process each partition independently.
```

---

## 26. Multi-threaded Step vs Partitioning

| Multi-threaded Step | Partitioning |
|---|---|
| threads share same step definition | each partition has its own execution context |
| simpler | better restart metadata |
| reader/writer must be thread-safe | cleaner data slicing |
| useful for simple parallelism | better for large production jobs |

Interview line:

```text
For serious restartable large-data processing, partitioning is usually easier to reason
about than sharing a reader across many threads.
```

---

## 27. Scheduling Spring Batch

Spring Batch does not require a scheduler.

A job can be launched by:
- command line
- REST endpoint
- Kubernetes CronJob
- enterprise scheduler
- Spring `@Scheduled`

Example:

```java
@Component
class SettlementScheduler {
    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    SettlementScheduler(JobLauncher jobLauncher, Job settlementJob) {
        this.jobLauncher = jobLauncher;
        this.settlementJob = settlementJob;
    }

    @Scheduled(cron = "0 0 1 * * *")
    void runDailySettlement() throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate("businessDate", LocalDate.now().minusDays(1))
                .toJobParameters();

        jobLauncher.run(settlementJob, parameters);
    }
}
```

Production caution:

```text
In a multi-instance app, @Scheduled may run on every instance unless protected by a
distributed lock or deployed as a single scheduler.
```

---

## 28. File Processing Pattern

Safe file processing:

```text
incoming/transactions.csv
processing/transactions.csv
archive/transactions.csv
failed/transactions.csv
```

Steps:
1. Move file to processing directory.
2. Validate header/trailer.
3. Read and process records.
4. Write output to temporary destination.
5. Commit database changes.
6. Atomically move output/archive files.
7. Store audit counts.

Common trap:

```text
Processing directly from incoming folder.
```

Better:

```text
Move to processing first so another job does not pick it up.
```

---

## 29. Database Processing Pattern

Common pattern:

```sql
select *
from payments
where status = 'READY'
order by id;
```

Production concerns:
- use indexes on status and id
- process by stable ordering
- avoid offset pagination for huge tables
- prefer keyset/range partitioning
- mark records as processing/done safely
- handle stuck processing records

Strong answer:

```text
For database batch jobs, I prefer stable ordering and range/keyset processing. I avoid
large offset scans and enforce idempotency with status transitions or unique constraints.
```

---

## 30. Remote API Calls In Batch

Batch jobs sometimes call external APIs.

Risks:
- slow runtime
- rate limits
- partial failure
- duplicate external side effects
- no easy rollback after API call

Controls:
- timeouts
- retry with backoff
- circuit breaker
- idempotency key
- throttling
- write-audit before/after call
- separate API side effects into their own step

Interview line:

```text
Remote calls inside batch must be treated carefully because database rollback cannot undo
an external side effect.
```

---

## 31. Production Monitoring

Monitor:
- job status
- step status
- start/end time
- duration
- read count
- write count
- skip count
- retry count
- failure exceptions
- stuck executions
- throughput records/sec

Operational alerts:
- job failed
- job not started on time
- job running too long
- skip count exceeds threshold
- output count differs from expected
- duplicate job instance attempted

---

## 32. Marriott Scenario: Nightly Booking Settlement

Requirement:

```text
Every night, process all successful hotel payments from yesterday and create settlement
records for finance. The job must not double-settle payments even if it fails and restarts.
```

Design:

1. Job parameter: `businessDate`.
2. Reader selects successful payments for that date.
3. Processor validates amount, currency, booking status.
4. Writer upserts settlement rows with unique `payment_id`.
5. Chunk size starts at 500.
6. Invalid records are skipped only for known validation errors.
7. Database deadlocks are retried.
8. JobRepository tracks progress.
9. Metrics expose read/write/skip counts.
10. Re-run with same `businessDate` is safe because writer is idempotent.

Strong interview answer:

```text
I would model this as a Spring Batch job with businessDate as the identifying parameter.
The main step reads yesterday's successful payments, validates them in a processor, and
writes settlements in chunks. I would enforce a unique key on payment_id so restart or retry
cannot double-settle. Known bad data can be skipped with a limit, transient DB failures can
be retried, and execution metadata from JobRepository gives auditability and restartability.
```

---

## 33. Hot Interview Questions

### Q1. Is Spring Batch a scheduler?

```text
No. A scheduler triggers work at a time. Spring Batch executes reliable batch workflows
with metadata, transactions, restartability, and skip/retry.
```

### Q2. What is chunk processing?

```text
Chunk processing reads, processes, and writes a group of items inside a transaction.
Each committed chunk becomes a safe checkpoint for restart.
```

### Q3. Why does Spring Batch need JobRepository?

```text
It stores metadata about job and step executions. This supports restart, audit, status
tracking, and failure debugging.
```

### Q4. What is the difference between JobInstance and JobExecution?

```text
JobInstance is the logical job run identified by job name and parameters. JobExecution
is one attempt to run that instance.
```

### Q5. How do you avoid duplicate records after restart?

```text
Use idempotent writes with unique business keys, upserts, processed markers, or external
idempotency keys. Do not rely only on memory.
```

### Q6. When would you use skip?

```text
For known bad records that business allows us to ignore temporarily, with skip limits and
reporting. I would not skip infrastructure failures blindly.
```

### Q7. When would you use retry?

```text
For transient errors such as deadlocks, network timeouts, and temporary database failures,
with strict retry limits and backoff.
```

### Q8. How do you scale a batch job?

```text
First tune queries, chunk size, indexes, and writers. Then consider parallel steps or
partitioning if data can be safely split.
```

---

## 34. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Treating batch as one big transaction | huge locks and rollback cost | transaction per chunk |
| No idempotency | restart duplicates side effects | unique keys/upserts/markers |
| Huge chunk size blindly | memory and lock issues | tune based on evidence |
| Skipping unknown exceptions | hides real failure | skip only known bad data |
| Retrying validation errors | wastes time | fail or skip based on business rule |
| Running scheduler on all app nodes | duplicate job launches | distributed lock or external scheduler |
| Calling remote APIs without timeout | stuck job | timeout, retry, idempotency |
| No metadata cleanup | metadata tables grow forever | retention policy |
| No skip report | bad records disappear | persist reject file/table |
| Non-restartable reader | restart starts wrong place | use restart-aware reader/checkpoint |

---

## 35. One-Hour Revision Plan

### First 15 Minutes: Architecture

Revise:
- Job
- Step
- JobLauncher
- JobRepository
- JobParameters
- JobInstance vs JobExecution

Must say:

```text
JobRepository is what turns a background process into an auditable and restartable batch job.
```

### Next 15 Minutes: Chunk Model

Revise:
- Reader
- Processor
- Writer
- chunk size
- transaction per chunk

Must say:

```text
Chunk processing gives memory control, checkpointing, and transaction boundaries.
```

### Next 15 Minutes: Failure Handling

Revise:
- restartability
- ExecutionContext
- skip
- retry
- rollback
- idempotency

Must say:

```text
Restartability is only safe when the writer and side effects are idempotent.
```

### Final 15 Minutes: Scaling

Revise:
- parallel steps
- partitioning
- scheduling
- monitoring
- remote API risks

Must say:

```text
I scale batch only after making the single-threaded path correct, restartable, observable,
and idempotent.
```

---

## 36. Final Rapid Revision Sheet

| Need | Spring Batch Concept |
|---|---|
| Whole workflow | Job |
| Stage inside workflow | Step |
| Start a job | JobLauncher |
| Store metadata | JobRepository |
| Identify logical run | JobParameters |
| One attempt | JobExecution |
| One stage attempt | StepExecution |
| Read data | ItemReader |
| Transform data | ItemProcessor |
| Write data | ItemWriter |
| Group transaction | Chunk |
| Simple single operation | Tasklet |
| Continue after failure | Restartability |
| Save checkpoint | ExecutionContext |
| Bad record handling | Skip |
| Transient failure handling | Retry |
| Split large input | Partitioning |
| Prevent duplicate side effects | Idempotency |

---

## 37. Strong Closing Answer

If interviewer asks:

```text
How strong are you in Spring Batch?
```

Say:

```text
I understand Spring Batch as a reliability framework for offline processing, not just a
scheduler. I can model jobs using Job, Step, JobRepository, JobParameters, and chunk-based
Reader/Processor/Writer flows. In production, I pay close attention to transaction boundaries,
restartability, idempotent writers, skip/retry policies, job metadata, monitoring, and scaling
through partitioning or parallel steps only when the data can be safely split.
```

---

## 38. Official Source Notes

Useful official references:

- Spring Batch Reference: https://docs.spring.io/spring-batch/reference/index.html
- Spring Boot Batch Applications: https://docs.spring.io/spring-boot/reference/io/spring-batch.html
- Spring Boot Task Execution and Scheduling: https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html

