import { useEffect } from 'react';
import { Button, Card, Form } from 'react-bootstrap';
import { SubmitHandler, useForm } from 'react-hook-form';
import { DataType, UploadParams } from '@/types.ts';
import { useToast } from '@/features/toast';
import { ToastType } from '@/features/toast/types.ts';

interface UploadFormInputs {
  file: any;
}

export const UploadFromFile = () => {
  const { showToast } = useToast();
  useEffect(() => {}, []);

  const { register, control, setValue, handleSubmit } =
    useForm<UploadFormInputs>({
      defaultValues: {
        file: null,
      },
    });

  const onSubmit: SubmitHandler<UploadFormInputs> = async (data) => {
    const params: UploadParams = {
      type: DataType.DEFAULT_CSV,
      mappings: 'Email:Email:tlc',
    };

    // const response = await upload(data.file[0], params);
    // console.log(response);
    showToast({
      title: 'test title',
      message: 'long test message',
      type: ToastType.DANGER,
    });
  };

  return (
    <Card>
      <Card.Body>
        <Card.Title>Ingest from file</Card.Title>

        <Form onSubmit={handleSubmit(onSubmit)}>
          <Form.Group className="mb-3" controlId="file">
            <Form.Label>File</Form.Label>
            <Form.Control as="input" type="file" {...register('file')} />
          </Form.Group>
          <Button variant="primary" type="submit">
            Upload
          </Button>
        </Form>
      </Card.Body>
    </Card>
  );
};
