This project shows how to finetune an LLM to a specific doc set using Java tools.

There are 5 components to this project.

1. The parser app takes a docset and converts it into separate sentences.  Using sentences now; we might switch to
another type, eg, html tags, in the future.   [see sentence parser in Utility.java]

2. The embeddings app takes those separate strings (sentences) and gets their OpenAI embeddings in JSON format
(presumably they use word2vec to generate the embeddings).     [see LLM.java]
   Note: create a file "llm.properties" in the src/main/resources folder.  Put your OpenAI key in that file
   and use the resource name llmservice.apikey, ie:

   llmservice.apikey=...your language model api key...     # your openai key
   llmservice.embedding_model=text-embedding-ada-002
   llmservice.completion_model=gpt-3.5-turbo
   llmservice.speech_model=whisper-1
   llmservice.maxtokensrequested=1024
   llmservice.temperature=0.8
   llmservice.percentsampled=1.00
   llmservice.numcompletions=1
   llmservice.stream=false
   llmservice.preamble=/Users/fgreco/src/Finetuning/src/main/resources/preamble-deepnetts.txt
   #llmservice.preamble=/Users/fgreco/src/Finetuning/src/main/resources/preamble-nyjavasig.txt
   vdbservice.database=...name of your database...
   vdbservice.collection=...name of your collection...
   vdbservice.port=19530
   vdbservice.host=localhost
   vdbservice.sentence_size=5120

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

Frank G