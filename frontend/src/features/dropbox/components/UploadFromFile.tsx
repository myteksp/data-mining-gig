import { Button, Card, Form, InputGroup, Spinner } from 'react-bootstrap';
import {
  Controller,
  SubmitHandler,
  useFieldArray,
  useForm,
} from 'react-hook-form';
import { DataType, UploadParams } from '@/types.ts';
import { useToast } from '@/features/toast';
import { ToastType } from '@/features/toast/types.ts';
import { upload } from './../api.ts';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { MdDelete } from 'react-icons/md';
import { useState } from 'react';

interface UploadFormInputs {
  file: FileList;
  type: DataType;
  mappings: MappingInputs[];
}

interface MappingInputs {
  destination: string;
  source: string;
  transformation: string;
}

const schema = yup.object().shape({
  type: yup
    .mixed<DataType>()
    .oneOf(Object.values(DataType))
    .required('Type is required'),
  file: yup
    .mixed<FileList>()
    .test({
      test: (value) => {
        console.log('validation', value?.length);
        // return context.createError({ message: 'Yolo 2' });
        return !!value?.length;
      },
      message: 'File is required',
    })
    .required(),
  mappings: yup.mixed<MappingInputs[]>().required(),
});

export const UploadFromFile = () => {
  const { showToast } = useToast();
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      type: DataType.DEFAULT_CSV,
      mappings: [{ destination: '', source: '', transformation: '' }],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'mappings',
  });

  const onSubmit: SubmitHandler<UploadFormInputs> = async (data) => {
    console.log(data);

    const mappings: string[] = [];

    data.mappings.map((item) => {
      mappings.push(
        `${item.destination}:${item.source}:${item.transformation}`,
      );
    });

    const params: UploadParams = {
      type: data.type,
      mappings: mappings,
    };

    setIsLoading(true);

    try {
      const response = await upload(data.file[0], params);

      if (response.status == 200) {
        showToast({
          title: 'Success!',
          message: 'File uploaded successfully',
          type: ToastType.SUCCESS,
        });
      }
      if (response.status == 208) {
        showToast({
          title: 'Error!',
          message: response.data.error,
          type: ToastType.WARNING,
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

  const addMappingField = () => {
    append({ destination: '', source: '', transformation: '' });
  };

  return (
    <Card>
      <Card.Body>
        <Card.Title>Ingest from file</Card.Title>

        <Form onSubmit={handleSubmit(onSubmit)}>
          <Form.Group className="mb-3" controlId="type">
            <Form.Label>Type</Form.Label>
            <Controller
              name="type"
              control={control}
              render={({ field }) => (
                <Form.Select {...field}>
                  {Object.keys(DataType).map((item) => {
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

          <Form.Group className="mb-3">
            <Form.Label>Mappings</Form.Label>

            {fields.map((item, index) => (
              <InputGroup key={item.id} className="mb-3">
                <Form.Control
                  id={`destination-${item.id}`}
                  placeholder={'Destination name of the column'}
                  {...register(`mappings.${index}.destination`)}
                />
                <Form.Control
                  id={`source-${item.id}`}
                  placeholder={'Source name of the column'}
                  {...register(`mappings.${index}.source`)}
                />
                <Form.Control
                  id={`transformation-${item.id}`}
                  placeholder={'Transformation'}
                  {...register(`mappings.${index}.transformation`)}
                />
                <Button
                  variant="outline-secondary"
                  onClick={() => remove(index)}
                >
                  <MdDelete />
                </Button>
              </InputGroup>
            ))}

            <div>
              <Button
                variant={'secondary'}
                type={'button'}
                onClick={addMappingField}
              >
                Add item
              </Button>
            </div>
          </Form.Group>

          <Form.Group className="mb-3" controlId="file">
            <Form.Label>File</Form.Label>
            <Form.Control
              as="input"
              type="file"
              isInvalid={!!errors.file}
              {...register('file')}
            />
            <Form.Control.Feedback type="invalid">
              {errors.file?.message}
            </Form.Control.Feedback>
          </Form.Group>

          {!isLoading && (
            <Button variant="primary" type="submit">
              Upload
            </Button>
          )}

          {isLoading && (
            <Button variant="primary" disabled>
              <Spinner as="span" animation="border" size="sm" role="status" />
              <span className="ms-1">Loading...</span>
            </Button>
          )}
        </Form>
      </Card.Body>
    </Card>
  );
};
