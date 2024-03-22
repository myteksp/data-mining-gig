import { Button, Card, Form, InputGroup, Spinner } from 'react-bootstrap';
import { useState } from 'react';
import { uploadFromDropbox } from '@/features/dropbox/api.ts';
import { DataType, UploadParams } from '@/types.ts';
import * as yup from 'yup';
import { useToast } from '@/features/toast';
import {
  Controller,
  SubmitHandler,
  useFieldArray,
  useForm,
} from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { ToastType } from '@/features/toast/types.ts';
import { MdDelete } from 'react-icons/md';
import { DropboxListModal } from '@/features/dropbox/components/DropboxListModal.tsx';

interface UploadFormInputs {
  path: string;
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
  mappings: yup
    .array()
    .of(
      yup.object().shape({
        destination: yup.string().required('Mappings is required'),
        source: yup.string().required('Mappings is required'),
        transformation: yup.string().required('Mappings is required'),
      }),
    )
    .min(1, 'Mappings is required')
    .required(),
  path: yup.string().required('Path is required'),
});

export const UploadFromDropbox = () => {
  const { showToast } = useToast();
  const [showDropboxModal, setShowDropboxModal] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    getValues,
    control,
    handleSubmit,
    setValue,
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
      const response = await uploadFromDropbox(data.path, params);

      if (response.status == 200) {
        showToast({
          title: 'Success!',
          message: 'File from Dropbox uploaded successfully',
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

  const handleCloseDropboxModal = () => {
    setShowDropboxModal(false);
  };
  const handleShowDropboxModal = () => {
    setShowDropboxModal(true);
  };

  const updatePathField = (value: string) => {
    setValue('path', value, { shouldValidate: true });
  };

  const addMappingField = () => {
    append({ destination: '', source: '', transformation: '' });
  };

  return (
    <Card>
      <Card.Body>
        <Card.Title>Ingest from Dropbox</Card.Title>

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
                  isInvalid={
                    !!(errors.mappings && errors.mappings[index]?.destination)
                  }
                  {...register(`mappings.${index}.destination`)}
                />
                <Form.Control
                  id={`source-${item.id}`}
                  placeholder={'Source name of the column'}
                  isInvalid={
                    !!(errors.mappings && errors.mappings[index]?.source)
                  }
                  {...register(`mappings.${index}.source`)}
                />
                <Form.Control
                  id={`transformation-${item.id}`}
                  placeholder={'Transformation'}
                  isInvalid={
                    !!(
                      errors.mappings && errors.mappings[index]?.transformation
                    )
                  }
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

            {!getValues('mappings').length && (
              <div className={'invalid-feedback d-block mb-3'}>
                {errors.mappings?.root?.message}
              </div>
            )}

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

          <Form.Group className="mb-3" controlId="path">
            <Form.Label>Path</Form.Label>
            <Controller
              name="path"
              control={control}
              render={({ field }) => (
                <InputGroup>
                  <Form.Control {...field} isInvalid={!!errors.path} />
                  <Button
                    variant="outline-secondary"
                    onClick={handleShowDropboxModal}
                  >
                    Browse
                  </Button>
                  <Form.Control.Feedback type="invalid">
                    {errors.path?.message}
                  </Form.Control.Feedback>
                </InputGroup>
              )}
            />
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

        <DropboxListModal
          show={showDropboxModal}
          onClose={handleCloseDropboxModal}
          onFileSelect={updatePathField}
        />
      </Card.Body>
    </Card>
  );
};
