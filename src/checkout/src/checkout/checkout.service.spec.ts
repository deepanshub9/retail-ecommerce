/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

import { Test, TestingModule } from '@nestjs/testing';
import { CheckoutService } from './checkout.service';
import { ICheckoutRepository } from './repositories';
import { IOrdersService } from './orders';
import { IShippingService } from './shipping';
import { CheckoutRequest } from './models/CheckoutRequest';
import { Checkout } from './models/Checkout';
import { ShippingRates } from './models/ShippingRates';

describe('CheckoutService', () => {
  let service: CheckoutService;
  let mockCheckoutRepository: jest.Mocked<ICheckoutRepository>;
  let mockOrdersService: jest.Mocked<IOrdersService>;
  let mockShippingService: jest.Mocked<IShippingService>;

  beforeEach(async () => {
    const mockRepository = {
      get: jest.fn(),
      set: jest.fn(),
      remove: jest.fn(),
    };

    const mockOrders = {
      create: jest.fn(),
    };

    const mockShipping = {
      getShippingRates: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        CheckoutService,
        {
          provide: 'CheckoutRepository',
          useValue: mockRepository,
        },
        {
          provide: 'OrdersService',
          useValue: mockOrders,
        },
        {
          provide: 'ShippingService',
          useValue: mockShipping,
        },
      ],
    }).compile();

    service = module.get<CheckoutService>(CheckoutService);
    mockCheckoutRepository = module.get('CheckoutRepository');
    mockOrdersService = module.get('OrdersService');
    mockShippingService = module.get('ShippingService');
  });

  describe('get', () => {
    it('should return null when checkout not found', async () => {
      // Given
      mockCheckoutRepository.get.mockResolvedValue(null);

      // When
      const result = await service.get('customer123');

      // Then
      expect(result).toBeNull();
      expect(mockCheckoutRepository.get).toHaveBeenCalledWith('customer123');
    });

    it('should return deserialized checkout when found', async () => {
      // Given
      const checkoutData = {
        items: [{ id: 'item1', quantity: 2, price: 10.99 }],
        subtotal: 21.98,
        tax: 5,
        shipping: 10,
        total: 36.98,
      };
      mockCheckoutRepository.get.mockResolvedValue(JSON.stringify(checkoutData));

      // When
      const result = await service.get('customer123');

      // Then
      expect(result).toBeDefined();
      expect(result.subtotal).toBe(21.98);
      expect(result.total).toBe(36.98);
    });
  });

  describe('update', () => {
    it('should calculate totals correctly for single item', async () => {
      // Given
      const request: CheckoutRequest = {
        items: [
          {
            id: 'product1',
            name: 'Test Product',
            quantity: 2,
            price: 15.99,
          },
        ],
        shippingAddress: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          address1: '123 Main St',
          city: 'Seattle',
          state: 'WA',
          zip: '98101',
        },
        deliveryOptionToken: 'standard',
      };

      const shippingRates: ShippingRates = {
        rates: [
          { token: 'standard', name: 'Standard', amount: 5.99 },
          { token: 'express', name: 'Express', amount: 12.99 },
        ],
      };

      mockShippingService.getShippingRates.mockResolvedValue(shippingRates);
      mockCheckoutRepository.set.mockResolvedValue();

      // When
      const result = await service.update('customer123', request);

      // Then
      expect(result.subtotal).toBe(31.98); // 2 * 15.99
      expect(result.tax).toBe(5);
      expect(result.shipping).toBe(5.99);
      expect(result.total).toBe(42.97); // 31.98 + 5 + 5.99
      expect(result.items).toHaveLength(1);
      expect(result.items[0].totalCost).toBe(31.98);
    });

    it('should calculate totals correctly for multiple items', async () => {
      // Given
      const request: CheckoutRequest = {
        items: [
          { id: 'product1', name: 'Product 1', quantity: 2, price: 10.00 },
          { id: 'product2', name: 'Product 2', quantity: 1, price: 25.50 },
          { id: 'product3', name: 'Product 3', quantity: 3, price: 8.99 },
        ],
        shippingAddress: {
          firstName: 'Jane',
          lastName: 'Smith',
          email: 'jane@example.com',
          address1: '456 Oak Ave',
          city: 'Portland',
          state: 'OR',
          zip: '97201',
        },
        deliveryOptionToken: 'express',
      };

      const shippingRates: ShippingRates = {
        rates: [
          { token: 'standard', name: 'Standard', amount: 7.99 },
          { token: 'express', name: 'Express', amount: 15.99 },
        ],
      };

      mockShippingService.getShippingRates.mockResolvedValue(shippingRates);
      mockCheckoutRepository.set.mockResolvedValue();

      // When
      const result = await service.update('customer456', request);

      // Then
      expect(result.subtotal).toBe(72.47); // (2*10) + 25.50 + (3*8.99)
      expect(result.tax).toBe(5);
      expect(result.shipping).toBe(15.99);
      expect(result.total).toBe(93.46);
      expect(result.items).toHaveLength(3);
    });

    it('should handle checkout without shipping address', async () => {
      // Given
      const request: CheckoutRequest = {
        items: [
          { id: 'product1', name: 'Digital Product', quantity: 1, price: 29.99 },
        ],
        shippingAddress: null,
        deliveryOptionToken: null,
      };

      mockCheckoutRepository.set.mockResolvedValue();

      // When
      const result = await service.update('customer789', request);

      // Then
      expect(result.subtotal).toBe(29.99);
      expect(result.tax).toBe(-1); // No tax without address
      expect(result.shipping).toBe(-1); // No shipping without address
      expect(result.total).toBe(29.99); // Only subtotal
      expect(mockShippingService.getShippingRates).not.toHaveBeenCalled();
    });

    it('should generate payment tokens', async () => {
      // Given
      const request: CheckoutRequest = {
        items: [{ id: 'product1', name: 'Test', quantity: 1, price: 10.00 }],
        shippingAddress: null,
        deliveryOptionToken: null,
      };

      mockCheckoutRepository.set.mockResolvedValue();

      // When
      const result = await service.update('customer123', request);

      // Then
      expect(result.paymentId).toBeDefined();
      expect(result.paymentId).toHaveLength(16);
      expect(result.paymentToken).toBeDefined();
      expect(result.paymentToken).toHaveLength(32);
    });

    it('should persist checkout data', async () => {
      // Given
      const request: CheckoutRequest = {
        items: [{ id: 'product1', name: 'Test', quantity: 1, price: 10.00 }],
        shippingAddress: null,
        deliveryOptionToken: null,
      };

      mockCheckoutRepository.set.mockResolvedValue();

      // When
      await service.update('customer123', request);

      // Then
      expect(mockCheckoutRepository.set).toHaveBeenCalledWith(
        'customer123',
        expect.any(String)
      );
    });
  });

  describe('submit', () => {
    it('should submit checkout and create order', async () => {
      // Given
      const checkout: Checkout = {
        items: [
          {
            id: 'product1',
            name: 'Test Product',
            quantity: 2,
            price: 15.99,
            totalCost: 31.98,
          },
        ],
        shippingAddress: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          address1: '123 Main St',
          city: 'Seattle',
          state: 'WA',
          zip: '98101',
        },
        subtotal: 31.98,
        tax: 5,
        shipping: 5.99,
        total: 42.97,
        paymentId: 'pay123',
        paymentToken: 'token456',
        deliveryOptionToken: 'standard',
        shippingRates: null,
      };

      const createdOrder = {
        id: 'order123',
        customerId: 'customer123',
        items: checkout.items,
        shippingAddress: checkout.shippingAddress,
        total: checkout.total,
      };

      mockCheckoutRepository.get.mockResolvedValue(JSON.stringify(checkout));
      mockOrdersService.create.mockResolvedValue(createdOrder);
      mockCheckoutRepository.remove.mockResolvedValue();

      // When
      const result = await service.submit('customer123');

      // Then
      expect(result.orderId).toBe('order123');
      expect(result.email).toBe('john@example.com');
      expect(result.total).toBe(42.97);
      expect(mockOrdersService.create).toHaveBeenCalledWith(checkout);
      expect(mockCheckoutRepository.remove).toHaveBeenCalledWith('customer123');
    });

    it('should throw error when checkout not found', async () => {
      // Given
      mockCheckoutRepository.get.mockResolvedValue(null);

      // When & Then
      await expect(service.submit('customer123')).rejects.toThrow(
        'Checkout not found'
      );
      expect(mockOrdersService.create).not.toHaveBeenCalled();
      expect(mockCheckoutRepository.remove).not.toHaveBeenCalled();
    });

    it('should handle order service errors', async () => {
      // Given
      const checkout: Checkout = {
        items: [{ id: 'product1', name: 'Test', quantity: 1, price: 10, totalCost: 10 }],
        shippingAddress: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          address1: '123 Main St',
          city: 'Seattle',
          state: 'WA',
          zip: '98101',
        },
        subtotal: 10,
        tax: 5,
        shipping: 5,
        total: 20,
        paymentId: 'pay123',
        paymentToken: 'token456',
        deliveryOptionToken: 'standard',
        shippingRates: null,
      };

      mockCheckoutRepository.get.mockResolvedValue(JSON.stringify(checkout));
      mockOrdersService.create.mockRejectedValue(new Error('Order service unavailable'));

      // When & Then
      await expect(service.submit('customer123')).rejects.toThrow(
        'Order service unavailable'
      );
      expect(mockCheckoutRepository.remove).not.toHaveBeenCalled();
    });
  });

  describe('edge cases', () => {
    it('should handle empty items array', async () => {
      // Given
      const request: CheckoutRequest = {
        items: [],
        shippingAddress: null,
        deliveryOptionToken: null,
      };

      mockCheckoutRepository.set.mockResolvedValue();

      // When
      const result = await service.update('customer123', request);

      // Then
      expect(result.subtotal).toBe(0);
      expect(result.items).toHaveLength(0);
      expect(result.total).toBe(0);
    });

    it('should handle invalid delivery option token', async () => {
      // Given
      const request: CheckoutRequest = {
        items: [{ id: 'product1', name: 'Test', quantity: 1, price: 10.00 }],
        shippingAddress: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          address1: '123 Main St',
          city: 'Seattle',
          state: 'WA',
          zip: '98101',
        },
        deliveryOptionToken: 'invalid-token',
      };

      const shippingRates: ShippingRates = {
        rates: [
          { token: 'standard', name: 'Standard', amount: 5.99 },
        ],
      };

      mockShippingService.getShippingRates.mockResolvedValue(shippingRates);
      mockCheckoutRepository.set.mockResolvedValue();

      // When
      const result = await service.update('customer123', request);

      // Then
      expect(result.shipping).toBe(-1); // Invalid token results in -1
      expect(result.total).toBe(15.00); // 10 + 5 (tax) + 0 (no shipping)
    });
  });
});