/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

import { Test, TestingModule } from '@nestjs/testing';
import { InMemoryCheckoutRepository } from './InMemoryCheckoutRepository';
import { RedisCheckoutRepository } from './RedisCheckoutRepository';
import { ICheckoutRepository } from './ICheckoutRepository';

describe('Checkout Repositories', () => {
  describe('InMemoryCheckoutRepository', () => {
    let repository: ICheckoutRepository;

    beforeEach(async () => {
      const module: TestingModule = await Test.createTestingModule({
        providers: [InMemoryCheckoutRepository],
      }).compile();

      repository = module.get<InMemoryCheckoutRepository>(InMemoryCheckoutRepository);
    });

    it('should store and retrieve checkout data', async () => {
      // Given
      const customerId = 'customer123';
      const checkoutData = JSON.stringify({
        items: [{ id: 'product1', quantity: 2, price: 10.99 }],
        total: 21.98,
      });

      // When
      await repository.set(customerId, checkoutData);
      const result = await repository.get(customerId);

      // Then
      expect(result).toBe(checkoutData);
    });

    it('should return null for non-existent checkout', async () => {
      // When
      const result = await repository.get('nonexistent');

      // Then
      expect(result).toBeNull();
    });

    it('should remove checkout data', async () => {
      // Given
      const customerId = 'customer123';
      const checkoutData = JSON.stringify({ total: 100 });
      await repository.set(customerId, checkoutData);

      // When
      await repository.remove(customerId);
      const result = await repository.get(customerId);

      // Then
      expect(result).toBeNull();
    });

    it('should handle multiple customers', async () => {
      // Given
      const customer1 = 'customer1';
      const customer2 = 'customer2';
      const data1 = JSON.stringify({ total: 100 });
      const data2 = JSON.stringify({ total: 200 });

      // When
      await repository.set(customer1, data1);
      await repository.set(customer2, data2);

      // Then
      expect(await repository.get(customer1)).toBe(data1);
      expect(await repository.get(customer2)).toBe(data2);
    });

    it('should overwrite existing data', async () => {
      // Given
      const customerId = 'customer123';
      const originalData = JSON.stringify({ total: 100 });
      const updatedData = JSON.stringify({ total: 200 });

      // When
      await repository.set(customerId, originalData);
      await repository.set(customerId, updatedData);
      const result = await repository.get(customerId);

      // Then
      expect(result).toBe(updatedData);
    });

    it('should handle concurrent operations', async () => {
      // Given
      const operations = Array(10).fill(null).map((_, index) => ({
        customerId: `customer${index}`,
        data: JSON.stringify({ total: index * 10 }),
      }));

      // When
      await Promise.all(
        operations.map(op => repository.set(op.customerId, op.data))
      );

      // Then
      const results = await Promise.all(
        operations.map(op => repository.get(op.customerId))
      );

      results.forEach((result, index) => {
        expect(result).toBe(operations[index].data);
      });
    });
  });

  describe('RedisCheckoutRepository', () => {
    let repository: ICheckoutRepository;
    let mockRedisClient: any;

    beforeEach(async () => {
      mockRedisClient = {
        get: jest.fn(),
        set: jest.fn(),
        del: jest.fn(),
        quit: jest.fn(),
      };

      const module: TestingModule = await Test.createTestingModule({
        providers: [
          {
            provide: RedisCheckoutRepository,
            useFactory: () => new RedisCheckoutRepository(mockRedisClient),
          },
        ],
      }).compile();

      repository = module.get<RedisCheckoutRepository>(RedisCheckoutRepository);
    });

    it('should store data in Redis with correct key', async () => {
      // Given
      const customerId = 'customer123';
      const checkoutData = JSON.stringify({ total: 100 });
      mockRedisClient.set.mockResolvedValue('OK');

      // When
      await repository.set(customerId, checkoutData);

      // Then
      expect(mockRedisClient.set).toHaveBeenCalledWith(
        'checkout:customer123',
        checkoutData
      );
    });

    it('should retrieve data from Redis', async () => {
      // Given
      const customerId = 'customer123';
      const checkoutData = JSON.stringify({ total: 100 });
      mockRedisClient.get.mockResolvedValue(checkoutData);

      // When
      const result = await repository.get(customerId);

      // Then
      expect(mockRedisClient.get).toHaveBeenCalledWith('checkout:customer123');
      expect(result).toBe(checkoutData);
    });

    it('should return null when Redis returns null', async () => {
      // Given
      mockRedisClient.get.mockResolvedValue(null);

      // When
      const result = await repository.get('nonexistent');

      // Then
      expect(result).toBeNull();
    });

    it('should remove data from Redis', async () => {
      // Given
      const customerId = 'customer123';
      mockRedisClient.del.mockResolvedValue(1);

      // When
      await repository.remove(customerId);

      // Then
      expect(mockRedisClient.del).toHaveBeenCalledWith('checkout:customer123');
    });

    it('should handle Redis connection errors', async () => {
      // Given
      mockRedisClient.get.mockRejectedValue(new Error('Redis connection failed'));

      // When & Then
      await expect(repository.get('customer123')).rejects.toThrow(
        'Redis connection failed'
      );
    });

    it('should handle Redis set errors', async () => {
      // Given
      mockRedisClient.set.mockRejectedValue(new Error('Redis write failed'));

      // When & Then
      await expect(repository.set('customer123', 'data')).rejects.toThrow(
        'Redis write failed'
      );
    });

    it('should use correct key prefix', async () => {
      // Given
      const customerId = 'test@example.com';
      mockRedisClient.get.mockResolvedValue(null);

      // When
      await repository.get(customerId);

      // Then
      expect(mockRedisClient.get).toHaveBeenCalledWith('checkout:test@example.com');
    });
  });

  describe('Repository Interface Compliance', () => {
    const repositories = [
      { name: 'InMemoryCheckoutRepository', factory: () => new InMemoryCheckoutRepository() },
    ];

    repositories.forEach(({ name, factory }) => {
      describe(name, () => {
        let repository: ICheckoutRepository;

        beforeEach(() => {
          repository = factory();
        });

        it('should implement ICheckoutRepository interface', () => {
          expect(repository.get).toBeDefined();
          expect(repository.set).toBeDefined();
          expect(repository.remove).toBeDefined();
        });

        it('should return promises from all methods', () => {
          const getResult = repository.get('test');
          const setResult = repository.set('test', 'data');
          const removeResult = repository.remove('test');

          expect(getResult).toBeInstanceOf(Promise);
          expect(setResult).toBeInstanceOf(Promise);
          expect(removeResult).toBeInstanceOf(Promise);
        });
      });
    });
  });
});