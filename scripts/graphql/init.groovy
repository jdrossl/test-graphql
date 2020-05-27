package graphql

import static graphql.Scalars.GraphQLInt
import static graphql.Scalars.GraphQLString
import static graphql.schema.GraphQLArgument.newArgument
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import static graphql.schema.GraphQLInterfaceType.newInterface
import static graphql.schema.GraphQLList.list
import static graphql.schema.GraphQLNonNull.nonNull
import static graphql.schema.GraphQLObjectType.newObject

// Define the fields common to all types
def entryFields = [
  newFieldDefinition()
    .name('Title')
    .description('The title of the entry')
    .type(nonNull(GraphQLString))
    .build(),
  newFieldDefinition()
    .name('Genre')
    .description('The genre of the entry')
    .type(nonNull(GraphQLString))
    .build(),
  newFieldDefinition()
    .name('Plot')
    .description('The plot of the entry')
    .type(nonNull(GraphQLString))
    .build(),
  newFieldDefinition()
    .name('Actors')
    .description('The main cast of the entry')
    .type(list(nonNull(GraphQLString)))
    .build()
]

// Define the parent type
def entryType = newInterface()
  .name('OmdbEntry')
  .description('The generic entry returned by the API')
  .fields(entryFields)
  .build()

// Define the type for movies
def movieType = newObject()
  .name('OmdbMovie')
  .description('The entry returned for movies by the API')
  // Use the parent type
  .withInterface(entryType)
  // GraphQL required to repeat all fields from the interface
  .fields(entryFields)
  .field(newFieldDefinition()
    .name('Production')
    .description('The studio of the entry')
    .type(nonNull(GraphQLString))
  )
  .build()

def seriesType = newObject()
  .name('OmdbSeries')
  .description('The entry returned for series by the API')
  // Use the parent type
  .withInterface(entryType)
  // GraphQL required to repeat all fields from the interface
  .fields(entryFields)
  .field(newFieldDefinition()
    .name('totalSeasons')
    .description('The number of seasons of the entry')
    .type(nonNull(GraphQLInt))
  )
  .build()

// Add the resolver for the new types
schema.resolver('OmdbEntry', { env ->
  // The API returns the type as a field
  switch(env.object.Type) {
    case 'movie':
      return movieType
    case 'series':
      return seriesType
  }
})

// Add the child types to the schema
// (this is needed because they are not used directly in any field)
schema.additionalTypes(movieType, seriesType)

// Add the new fields to the top level type
schema.field(newFieldDefinition()
  .name('omdb') // this field is used to wrap the service calls
  .description('All operations related to the OMDb API')
  .type(newObject() // inline type definition
    .name('OmdbService')
    .description('Exposes the OMDb Service')
    .field(newFieldDefinition()
      .name('search')
      .description('Performs a search by title')
      // uses the parent type, the resolver will define the concrete type
      .type(list(nonNull(entryType)))
      .argument(newArgument()
        .name('title')
        .description("The title to search")
        .type(GraphQLString)
      )
    )
  )
)

// Add the fetcher for the search field,
schema.fetcher('OmdbService', 'search', { env ->
  // calls the Groovy bean passing the needed parameters
  applicationContext.omdbService.search(env.getArgument('title'))
})

// Define a fetcher to split the value returned by the API for the Actors
def actorsFetcher = { env -> env.source.Actors?.split(',')*.trim() }

// Add the fetcher to the concrete types
schema.fetcher('OmdbMovie', 'Actors', actorsFetcher)
schema.fetcher('OmdbSeries', 'Actors', actorsFetcher)
