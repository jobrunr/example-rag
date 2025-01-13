# A RAG app powered by JobRunr

This repository provides a working RAG (retrieval-augmented generation) app powered by JobRunr and [Spring AI](https://spring.io/projects/spring-ai).

RAG helps you enhance the accuracy of large language models (LLMs) by providing the AI with the relevant domain knowledge at query time. This is made easy by 
Spring AI. Thanks to the features the library provides, Java developers can connect their app to the most popular LLMs models.

JobRunr can make developing a RAG app easier and more enjoyable, especially for enterprises where documents are in a large amount and frequently updated. 
JobRunr provides the tools to keep your embedding (or vector) store up to date with the changes happening in the document repository.

- **Background (distributed) batch processing**: embeddings can be updated in the background, thus the chat is not blocked while this is not happening. 
Need to process millions of documents? Scaling is easy thanks to the distributed processing.
- **Recurring jobs**: add logic to update the embeddings and execute it at regular timing with a recurring job.
- **Automatic retries**: anything that can go wrong will go wrong (👋network failure), thanks to automatic retries you'll not need to manually rewire your jobs.

Find more features at https://www.jobrunr.io/en/documentation/.

## About this example
This is a console app to chat with an LLM. At startup, the app will register a JobRunr `RecurringJob` to update embeddings for documents in 
a user configured folder. For each document found in, or missing from, the folder, a job is created to create/update or delete embeddings for the document. 
This updating of embeddings heavily relies on API's provided by Spring AI.

To avoid unnecessary computation, this implementation makes use of the last modification time of a file. If the file is new or changed, a job is created to update 
its embeddings. The app also automatically removes embeddings if a previously processed file is deleted from the configured folder.

### Configuration
```
app.content-dir=path/to/folder
app.embedding-synchronization.cron=0 0 * * *
app.similarity-threshold=0.25
```

You may clone the [JobRunr's documentation repository](https://github.com/jobrunr/website) and use the `documentation` folder as domain knowledge to trial this app. 
You can change the synchronization cron expression to something more fitting, you expect you docs to be updated every hour? Change the cron to hourly!

### Supported file formats
This example support `.md`, `.pdf` and `.txt`. You can easily extend this list by implementing [ContentProcessor](src/main/java/org/jobrunr/examples/embedding/service/ContentProcessor.java). 
The processing of these documents entirely relies on Spring AI capabilities. There are room for improvement (e.g., cleanup, better chunk sizes, etc.).

### How to use

- Start database
```shell
docker run -it --rm --name postgres -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres pgvector/pgvector:pg17
```
- Start [RagConsoleApplication](src/main/java/org/jobrunr/examples/RagConsoleApplication.java)
- Head over to `localhost:8000/recurring-jobs` and trigger the recurring job to generate initial embeddings, you can avoid this step by reducing the cron 
(see Configuration) or by running the `org.jobrunr.examples.embedding.service.DirectoryManager.manage` on startup of the app.
- Wait a bit for the embedding to be generated before asking your questions.

### Room for improvement
We provide this code to highlight what JobRunr can provide to RAG applications. 
You should feel free to adapt it to your use case and make it more practical for end-users. 
