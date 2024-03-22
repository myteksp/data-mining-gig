import { Button, Card, Col, Form, Row } from 'react-bootstrap';
import { useEffect, useState } from 'react';
import { getExportToDropbox, getMappings, getSearch } from './../api.ts';
import { Controller, SubmitHandler, useForm } from 'react-hook-form';
import {
  EnrichmentMethod,
  ExportParams,
  FilterType,
  SearchParams,
} from '@/types.ts';
import { SearchResults } from './SearchResults.tsx';
import { ToastType } from '@/features/toast/types.ts';
import { useToast } from '@/features/toast';

interface FormFormInputs {
  startNode: string;
  queryField: FilterType;
  joinOn: string;
  enrichmentMode: EnrichmentMethod;
  enrichmentDepthNumber: number;
  skipNumber: number;
  limitNumber: number;
  queryString: string;
  path: string;
}

export const SearchAndExport = () => {
  const { showToast } = useToast();

  const [mappings, setMappings] = useState<string[] | null>(null);
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isExport, setIsExport] = useState(false);

  useEffect(() => {
    getMappings().then((value) => {
      setMappings(value.data);
      setValue('joinOn', value.data[0]);
    });
  }, []);

  const { control, setValue, handleSubmit } = useForm<FormFormInputs>({
    defaultValues: {
      startNode: 'ALL',
      queryField: FilterType.CONTAINS,
      joinOn: '',
      enrichmentMode: EnrichmentMethod.DEEP,
      enrichmentDepthNumber: 10,
      skipNumber: 0,
      limitNumber: 100,
      queryString: '',
      path: '',
    },
  });

  const onSubmit: SubmitHandler<FormFormInputs> = (data) => {
    if (isExport) {
      onExport(data);
    } else {
      onSearch(data);
    }
  };

  const onSearch = (data: FormFormInputs) => {
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

  const onExport = async (data: FormFormInputs) => {
    const params: ExportParams = {
      filter: data.queryString,
      filterType: data.queryField,
      enrichmentMethod: data.enrichmentMode,
      joinOn: data.joinOn,
      maxDepth: data.enrichmentDepthNumber,
      path: data.path,
    };

    if (data.startNode !== 'ALL') {
      params.recordType = data.startNode;
    }

    setIsLoading(true);

    try {
      const response = await getExportToDropbox(params);

      if (response.status == 200) {
        showToast({
          title: 'Success!',
          message: 'Export to Dropbox completed successfully',
          type: ToastType.SUCCESS,
        });
      }
      setIsLoading(false);
    } catch (error) {
      console.error(error);

      if (error instanceof Error) {
        showToast({
          title: 'Error',
          message: error.message,
          type: ToastType.DANGER,
        });
      }
      setIsLoading(false);
    }
  };

  return (
    <Card>
      <Card.Body>
        <Card.Title>Search and Export</Card.Title>

        <Form onSubmit={handleSubmit(onSubmit)} className={'mb-3'}>
          <Row>
            <Col>
              <Form.Group className="mb-3" controlId="start-node">
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
              <Form.Group className="mb-3" controlId="query-field">
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
              <Form.Group className="mb-3" controlId="join-on">
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
              <Form.Group className="mb-3" controlId="enrichment-mode">
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
              <Form.Group className="mb-3" controlId="enrichment-depth-number">
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
              <Form.Group className="mb-3" controlId="query-string">
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

          {!isExport && (
            <Row>
              <Col>
                <Form.Group className="mb-3" controlId="skip-number">
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
              <Col>
                <Form.Group className="mb-3" controlId="limit-number">
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
            </Row>
          )}

          {isExport && (
            <Form.Group className="mb-3" controlId="path">
              <Form.Label>Path</Form.Label>
              <Controller
                name="path"
                control={control}
                render={({ field }) => <Form.Control {...field} type="text" />}
              />
            </Form.Group>
          )}

          <Form.Group className="mb-3">
            <Form.Check
              defaultChecked={isExport}
              type="switch"
              label="Is export?"
              onChange={(event) => setIsExport(event.target.checked)}
            />
          </Form.Group>

          <Button variant="primary" type="submit" disabled={isLoading}>
            {isExport ? 'Export to Dropbox' : 'Search'}
          </Button>
        </Form>

        {!isExport && (
          <SearchResults
            isLoading={isLoading}
            mappings={mappings}
            searchResults={searchResults}
          />
        )}
      </Card.Body>
    </Card>
  );
};
