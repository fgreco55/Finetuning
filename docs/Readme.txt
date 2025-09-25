This repo is not about "fine-tuning" in the formal sense.  This repo was created back in the days where the term "fine-tuning" was used loosely.
"fine-tuning" now implies changing the weights in the neural network.  This repo does NOT do this.  It was a learning exercise, so many aspects of this repo would be significantly changed today.  Use this repo at your own risk.

This project shows how to get an LLM to understand a specific doc set using Java tools.
This is an early attempt at understanding context.  It is not true finetuning (ie, changing the weights in the LLM's NN).

There are 5 components to this project.

1. The parser app takes a docset and converts it into separate sentences.  Using sentences now; we might switch to
another type, eg, html tags, in the future.   [see sentence parser in Utility.java]

2. The embeddings app takes those separate strings (sentences) and gets their OpenAI embeddings in JSON format
(presumably they use word2vec to generate the embeddings).     [see LLM.java]
   Note: create a file "llm.properties" in the src/main/resources folder.

   # openai key
   llmservice.apikey=...your language model api key...

   llmservice.completion_model=gpt-3.5-turbo
   llmservice.embedding_model=text-embedding-ada-002
   llmservice.speech_model=whisper-1

   # completion length
   llmservice.maxtokensrequested=1024

   # creativity level 0.0->2.0
   llmservice.temperature=0.8
   llmservice.percentsampled=1.00
   # maybe investigate for future use
   llmservice.numcompletions=1
   # handle SSE or not
   llmservice.stream=false
   # timeout in seconds
   llmservice.timeout=60
   # embedding vector length - dependent on embedding model
   llmservice.vector_size=1536
   # size of the LRU - history is added to the user prompt
   llmservice.prompthistory=10

   # you could put an FAQ here
   llmservice.preamble=/Users/fgreco/src/Finetuning/src/main/resources/preamble-faq.txt
   # llm instructions on how to present the completion
   llmservice.instructions=/Users/fgreco/src/Finetuning/src/main/resources/system-instructions.txt
   # works in tandem with instructions
   llmservice.language=english
   # --------------------------------------------------------------------------------------------------------
   # default database
   vdbservice.database=frankdb
   # default collection (table)
   vdbservice.collection=trialcollection

   # dependent on Milvus config
   vdbservice.port=19530
   vdbservice.host=localhost
   # we're using sentences as 'chunks'.  This is the max char size (stored as VARCHAR)
   vdbservice.sentence_size=5120
   # how many semantic matches when comparing against user query
   vdbservice.maxmatches=5

3. The datastore app takes the embeddings (List of Doubles) created by OpenAI and puts them into a vector database.
For now, we are using Milvus since they have a decent Java API.    [see MilvusTest.java and still needs work]
    a. Here's how to install the Milvus SDK and the Milvus server:  https://milvus.io/docs/install-java.md
    b. Here's how to run the Milvus server from a Docker image: https://milvus.io/docs/install_standalone-docker.md

4. The end-user CLI app gets a user request and finds the top vector matches in the vector DB of that request.
It puts those matches into the preamble of a prompt, adds the user request, and then asks the
LLM (ChatGPT for now) for a completion.  This app could have cli options to optionally show the top matches, the user
input, and the completion from the LLM.     [still TBD]
    idea:  % pchat [file(s)] [directory(s)] [URL(s)]

5. The end-user GUI app does the same as the CLI, except there's a nice JavaFX GUI.     [still TBD]


Notes:
  FAQ type of files should not contain periods, question marks or exclamation marks.  You want the question and answer on the same line.
  Using a sentence delimiter will break apart the line, which is probably not a good thing.  You want the embedding vector for entire question and answer.
  Currently, the only period in a line should be the very last character.  If not, the sentence parser will include the \n and break the line at the CR.


Frank G
