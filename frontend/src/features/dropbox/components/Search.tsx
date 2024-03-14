import { Button, Card, Col, Form, Row } from 'react-bootstrap';
import { useEffect, useState } from 'react';
import { getMappings, getSearch } from './../api.ts';
import { Controller, SubmitHandler, useForm } from 'react-hook-form';
import { EnrichmentMethod, FilterType, SearchParams } from '@/types.ts';
import { SearchResults } from './SearchResults.tsx';

interface SearchFormInputs {
  startNode: string;
  queryField: FilterType;
  joinOn: string;
  enrichmentMode: EnrichmentMethod;
  enrichmentDepthNumber: number;
  skipNumber: number;
  limitNumber: number;
  queryString: string;
}

export const Search = () => {
  const [mappings, setMappings] = useState<string[] | null>(null);
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    getMappings().then((value) => {
      setMappings(value.data);
      setValue('joinOn', value.data[0]);
    });
  }, []);

  const { control, setValue, handleSubmit } = useForm<SearchFormInputs>({
    defaultValues: {
      startNode: 'ALL',
      queryField: FilterType.CONTAINS,
      joinOn: '',
      enrichmentMode: EnrichmentMethod.DEEP,
      enrichmentDepthNumber: 10,
      skipNumber: 0,
      limitNumber: 100,
      queryString: '',
    },
  });

  const onSubmit: SubmitHandler<SearchFormInputs> = (data) => {
    const params: SearchParams = {
      filter: data.queryString,
      filterType: data.queryField,
      enrichmentMethod: data.enrichmentMode,
      joinOn: data.joinOn,
      maxDepth: data.enrichmentDepthNumber,
      skip: data.skipNumber,
      limit: data.limitNumber,
    };

    if (data.startNode !== 'ALL') {
      params.recordType = data.startNode;
    }

    const batchSize = 10;
    const loopLimit =
      data.limitNumber < batchSize ? 1 : data.limitNumber / batchSize;

    setIsLoading(true);
    setSearchResults([]);

    for (let i = 0; i < loopLimit; i++) {
      params.skip = data.skipNumber + batchSize * i;
      params.limit = batchSize;

      getSearch(params).then((data) => {
        setSearchResults((prevState) => {
          return [...prevState, ...data.data];
        });
        setIsLoading(false);
      });
    }
  };

  return (
    <Card>
      <Card.Body>
        <Card.Title>Search</Card.Title>

        <Form onSubmit={handleSubmit(onSubmit)} className={'mb-3'}>
          <Row>
            <Col>
              <Form.Group className="mb-3" controlId="search-start-node">
                <Form.Label>Select column to search</Form.Label>
                <Controller
                  name="startNode"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      <option value="ALL">ALL</option>
                      {mappings !== null &&
                        mappings.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-query-field">
                <Form.Label>Select query type</Form.Label>
                <Controller
                  name="queryField"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      {Object.keys(FilterType).map((item) => {
                        return (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        );
                      })}
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-join-on">
                <Form.Label>
                  Select on which field to search commonalities
                </Form.Label>
                <Controller
                  name="joinOn"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      {mappings !== null &&
                        mappings.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
          </Row>

          <Row>
            <Col>
              <Form.Group className="mb-3" controlId="search-enrichment-mode">
                <Form.Label>Select enrichment mode</Form.Label>
                <Controller
                  name="enrichmentMode"
                  control={control}
                  render={({ field }) => (
                    <Form.Select {...field}>
                      {Object.keys(EnrichmentMethod).map((item) => {
                        return (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        );
                      })}
                    </Form.Select>
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group
                className="mb-3"
                controlId="search-enrichment-depth-number"
              >
                <Form.Label>Enrichment depth</Form.Label>
                <Controller
                  name="enrichmentDepthNumber"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-skip-number">
                <Form.Label>Skip</Form.Label>
                <Controller
                  name="skipNumber"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
          </Row>

          <Row>
            <Col>
              <Form.Group className="mb-3" controlId="search-limit-number">
                <Form.Label>Limit</Form.Label>
                <Controller
                  name="limitNumber"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
            <Col>
              <Form.Group className="mb-3" controlId="search-query-string">
                <Form.Label>Query</Form.Label>
                <Controller
                  name="queryString"
                  control={control}
                  render={({ field }) => (
                    <Form.Control {...field} type="text" />
                  )}
                />
              </Form.Group>
            </Col>
          </Row>

          <Button variant="primary" type="submit">
            Search
          </Button>
        </Form>

        <SearchResults
          isLoading={isLoading}
          mappings={mappings}
          searchResults={searchResults}
        />
      </Card.Body>
    </Card>
  );
};
